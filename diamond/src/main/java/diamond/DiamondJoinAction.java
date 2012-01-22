package diamond;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Items;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.remoting.Channel;
import hudson.tasks.BuildStep;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import diamond.util.FakeRun;
import diamond.util.ProjectsHelper;

/**
 * Build action that allow to trigger build of joined project and make some
 * extra actions after all downstream project build hierarchy was completed.
 * 
 * @author Julien Bouyoud
 */
@SuppressWarnings("rawtypes")
public class DiamondJoinAction implements Action {
	
	/** Class Logger */
	private static final Logger LOGGER = Logger
	        .getLogger(DiamondJoinAction.class.getName());
	
	/**
	 * Project that start build.
	 */
	private transient final String masterJoinProjectName;
	/**
	 * Build Id of root project.
	 */
	private transient final int masterJoinProjectBuildId;
	/**
	 * Diamond join Trigger that contains all needed infos for join Action.
	 */
	private transient final DiamondJoinTrigger diamondTrigger;
	/**
	 * List of all pending downstream projects
	 */
	private transient final List<AbstractProject<?, ?>> pendingDownstreamProjects;
	/**
	 * Flag indicates if Join Actions has ran
	 */
	private transient final AtomicBoolean isJoinActionRan;
	/**
	 * Joined Result
	 */
	private transient Result globalResult = Result.SUCCESS;
	
	/**
	 * Create a new JoinAction for specified build hierarchy
	 * 
	 * @param masterJoinBuild
	 *            master build to take care of
	 * @param diamondTrigger
	 *            {@link DiamondJoinTrigger} that contains all needed
	 *            configuration like :
	 *            {@link DiamondJoinTrigger#getJoinProjects()},
	 *            {@link DiamondJoinTrigger#getThreshold()},
	 *            {@link DiamondJoinTrigger#getPostJoinActions()}
	 */
	public DiamondJoinAction(
	        final AbstractBuild<? extends AbstractProject<?, ?>, ? extends AbstractBuild<?, ?>> masterJoinBuild,
	        final DiamondJoinTrigger diamondTrigger) {
		if (masterJoinBuild == null) {
			throw new IllegalArgumentException("masterJoinBuild == null");
		}
		if (diamondTrigger == null) {
			throw new IllegalArgumentException("diamondTrigger == null");
		}
		final List<AbstractProject<?, ?>> downstreamProjectsHierarchy = ProjectsHelper
		        .getDownstreamProjectsHierarchy(masterJoinBuild.getProject(),
		                false, false);
		downstreamProjectsHierarchy.add(masterJoinBuild.getProject());
		pendingDownstreamProjects = new CopyOnWriteArrayList<AbstractProject<?, ?>>(
		        downstreamProjectsHierarchy);
		for (final AbstractProject<?, ?> project : pendingDownstreamProjects) {
			if (project.isDisabled() || !project.isBuildable()) {
				pendingDownstreamProjects.remove(project);
			}
		}
		masterJoinProjectName = masterJoinBuild.getProject().getName();
		masterJoinProjectBuildId = masterJoinBuild.getNumber();
		this.diamondTrigger = diamondTrigger;
		isJoinActionRan = new AtomicBoolean(false);
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.Action#getIconFileName()
	 */
	@Override
	public String getIconFileName() {
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.Action#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return "DiamondJoin";
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.Action#getUrlName()
	 */
	@Override
	public String getUrlName() {
		return null;
	}
	
	/**
	 * Return if this action as already run.
	 * 
	 * @return <code>true</code> if this action is run, <code>false</code> else.
	 */
	public final boolean isCompleted() {
		return isJoinActionRan.get();
	}
	
	/**
	 * Return if this action points to itself.
	 * <p>
	 * Check if all joined projects was not in pending downstream hierarchy
	 * 
	 * @return <code>true</code> if this action has a pending downstream build
	 *         that points to a joined project, <code>false</code> else
	 */
	public final boolean pointsItself() {
		boolean result = false;
		final List<AbstractProject> allJoinedProjects = diamondTrigger
		        .getAllJoinProjects();
		// Est-ce que la chaine de dépendence contient un des projet joinné ( en
		// vérifiant bien dans toute l arbre de dépendances)
		for (final AbstractProject<?, ?> pendingDownStreamProject : pendingDownstreamProjects) {
			for (final AbstractProject<?, ?> joinedProject : allJoinedProjects) {
				if (ProjectsHelper.getDownstreamProjectsHierarchy(
				        joinedProject, false, true).contains(
				        pendingDownStreamProject)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}
	
	/**
	 * Add a Collection of upstream JoinActions dependencies
	 * 
	 * @param upstreamJoinActions
	 *            a Collection of upstream JoinActions dependencies
	 */
	public void addJoinDependencies(
	        final List<DiamondJoinAction> upstreamJoinActions) {
		if (upstreamJoinActions == null) {
			throw new IllegalArgumentException("upstreamJoinActions == null");
		}
		
		for (final DiamondJoinAction nestedJoinAction : upstreamJoinActions) {
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine("JoinAction for Build : '" + masterJoinProjectName
				        + "[" + masterJoinProjectBuildId + "] "
				        + "' has now upstreamJoinDependency to"
				        + nestedJoinAction.masterJoinProjectName + "["
				        + nestedJoinAction.masterJoinProjectBuildId + "] "
				        + " join to " + diamondTrigger.getJoinProjectsValue());
			}
			for (final AbstractProject<?, ?> project : diamondTrigger
			        .getJoinProjects()) {
				if (!project.isDisabled() && project.isBuildable()) {
					nestedJoinAction
					        .addNestedJoinProjectsDependencies(ProjectsHelper
					                .getDownstreamProjectsHierarchy(project,
					                        false, false));
				}
			}
		}
	}
	
	/**
	 * Add a Collection of AbstractProject to pendingDownstreamProjects
	 * 
	 * @param nestedJoinProjects
	 *            a Collection of AbstractProject
	 */
	protected void addNestedJoinProjectsDependencies(
	        final Collection<? extends AbstractProject> nestedJoinProjects) {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine(new StringBuilder(DiamondJoinAction.class
			        .getSimpleName()).append(" on build [")
			        .append(masterJoinProjectName).append("#")
			        .append(masterJoinProjectBuildId)
			        .append("] has new project dependencies ")
			        .append(Items.toNameList(nestedJoinProjects)).toString());
		}
		for (final AbstractProject<?, ?> joinProject : nestedJoinProjects) {
			if (!pendingDownstreamProjects.contains(joinProject)) {
				pendingDownstreamProjects.add(joinProject);
			}
		}
	}
	
	/**
	 * Notify action of build completion.
	 * <p>
	 * Build must be in root build hierarchy.
	 * <p>
	 * If pendingDownstreamProjectsHierarchy doesn't contains any pending
	 * project, start triggering Join Actions
	 * 
	 * @param build
	 *            project build that completed
	 * @param listener
	 *            build listener
	 */
	public final void notifyBuildCompletion(
	        final AbstractBuild<? extends AbstractProject<?, ?>, ? extends AbstractBuild<?, ?>> build,
	        final TaskListener listener) {
		if (build == null) {
			throw new IllegalArgumentException("build == null");
		}
		if (listener == null) {
			throw new IllegalArgumentException("listener == null");
		}
		if (pendingDownstreamProjects.contains(build.getProject())) {
			pendingDownstreamProjects.remove(build.getProject());
			globalResult = globalResult.combine(build.getResult());
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine("Join Build : " + masterJoinProjectName + "["
				        + masterJoinProjectBuildId + "] => Removing project "
				        + build.getProject().getName() + ", "
				        + pendingDownstreamProjects.size() + " project left.");
			}
		}
		
		if (pendingDownstreamProjects.isEmpty() && !isJoinActionRan.get()) {
			isJoinActionRan.getAndSet(true);
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine("Join Build : "
				        + masterJoinProjectName
				        + "["
				        + masterJoinProjectBuildId
				        + "] => All downstream projects complete! Start joinning tasks");
			}
			listener.getLogger().println("All downstream projects complete!");
			listener.getLogger().println(
			        " All projects results was : " + globalResult.toString());
			if (globalResult.isWorseThan(diamondTrigger.getThreshold())) {
				listener.getLogger().println(
				        "Minimum result threshold not met for join project");
			} else {
				startJoinningTasks(build, listener);
			}
		}
	}
	
	/**
	 * Start Join tasks if pending downstream build list is empty and if Join
	 * tasks was not already run.
	 * 
	 * @param build
	 *            project build that completed
	 * @param listener
	 *            build listener
	 */
	@SuppressWarnings("unchecked")
	private final void startJoinningTasks(
	        final AbstractBuild<? extends AbstractProject<?, ?>, ? extends AbstractBuild<?, ?>> build,
	        final TaskListener listener) {
		if (build == null) {
			throw new IllegalArgumentException("build == null");
		}
		if (listener == null) {
			throw new IllegalArgumentException("listener == null");
		}
		try {
			final AbstractProject masterProject = Hudson.getInstance()
			        .getItemByFullName(masterJoinProjectName,
			                AbstractProject.class);
			final FakeRun run = new FakeRun(
			        (AbstractBuild<?, ?>) masterProject
			                .getBuildByNumber(masterJoinProjectBuildId),
			        globalResult);
			
			// Start Join Projects
			for (final AbstractProject<?, ?> joinProject : diamondTrigger
			        .getAllJoinProjects()) {
				if (joinProject.isDisabled()) {
					listener.getLogger().println(
					        "Project " + joinProject.getName()
					                + " is disabled, skip join on.");
				} else {
					listener.getLogger()
					        .println(
					                "Scheduling join project: "
					                        + joinProject.getName());
					joinProject.scheduleBuild(joinProject.getQuietPeriod(),
					        new UpstreamCause((Run<?, ?>) run));
				}
			}
			// Start Post Build Actions...
			final Launcher launcher = new NoopLauncher(listener, build);
			
			listener.getLogger().println("Start post-build Actions...");
			for (final BuildStep pub : diamondTrigger.getPostJoinActions()) {
				try {
					// Sad but parameterizedtrigger didn't start job. so
					// do
					// it manually
					if (pub instanceof hudson.plugins.parameterizedtrigger.BuildTrigger) {
						final hudson.plugins.parameterizedtrigger.BuildTrigger trigger = (hudson.plugins.parameterizedtrigger.BuildTrigger) pub;
						for (final BuildTriggerConfig config : trigger
						        .getConfigs()) {
							config.perform(run, launcher,
							        (BuildListener) listener);
						}
					} else {
						pub.perform(run, launcher, (BuildListener) listener);
					}
				} catch (final InterruptedException e) {
					listener.getLogger().print(e.toString());
				} catch (final IOException e) {
					listener.getLogger().print(e.toString());
				}
			}
		} catch (final IOException e) {
			listener.getLogger().print(e.toString());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return masterJoinProjectName.hashCode() + masterJoinProjectBuildId
		        + diamondTrigger.getJoinProjectsValue().hashCode()
		        + diamondTrigger.getThreshold().hashCode()
		        + diamondTrigger.getPostJoinActions().hashCode();
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof DiamondJoinAction) {
			final DiamondJoinAction otherAction = (DiamondJoinAction) obj;
			return otherAction.masterJoinProjectName
			        .equals(masterJoinProjectName)
			        && otherAction.masterJoinProjectBuildId == masterJoinProjectBuildId
			        && otherAction.diamondTrigger.getJoinProjectsValue()
			                .equals(diamondTrigger.getJoinProjectsValue())
			        && otherAction.diamondTrigger.getThreshold().equals(
			                diamondTrigger.getThreshold())
			        && otherAction.diamondTrigger.getPostJoinActions().equals(
			                diamondTrigger.getPostJoinActions());
		}
		return super.equals(obj);
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new StringBuilder(DiamondJoinAction.class.getSimpleName())
		        .append(" on build [").append(masterJoinProjectName)
		        .append("#").append(masterJoinProjectBuildId)
		        .append("] wait completion of ")
		        .append(Items.toNameList(pendingDownstreamProjects))
		        .append(" before start new build of : ")
		        .append(diamondTrigger.getAllJoinProjectsValue()).toString();
	}
	
	/**
	 * No Operation Launcher
	 * 
	 * @see Launcher
	 */
	private static class NoopLauncher extends Launcher {
		
		/**
		 * Create a new NoopLauncher
		 * 
		 * @param listener
		 * @param build
		 */
		public NoopLauncher(final TaskListener listener,
		        final AbstractBuild<?, ?> build) {
			super(listener, build.getBuiltOn().getChannel());
		}
		
		/*
		 * (non-Javadoc)
		 * @see hudson.Launcher#launch(hudson.Launcher.ProcStarter)
		 */
		@Override
		public Proc launch(final ProcStarter starter) throws IOException {
			throw new UnsupportedOperationException("Not supported.");
		}
		
		/*
		 * (non-Javadoc)
		 * @see hudson.Launcher#launchChannel(java.lang.String[],
		 * java.io.OutputStream, hudson.FilePath, java.util.Map)
		 */
		@Override
		public Channel launchChannel(final String[] cmd,
		        final OutputStream out, final FilePath workDir,
		        final Map<String, String> envVars) throws IOException,
		        InterruptedException {
			throw new UnsupportedOperationException("Not supported.");
		}
		
		/*
		 * (non-Javadoc)
		 * @see hudson.Launcher#kill(java.util.Map)
		 */
		@Override
		public void kill(final Map<String, String> modelEnvVars)
		        throws IOException, InterruptedException {
			throw new UnsupportedOperationException("Not supported.");
		}
	}
}
