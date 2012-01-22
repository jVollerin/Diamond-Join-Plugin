package diamond;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receives notifications about builds.<br/>
 * {@link #onStarted(AbstractBuild, TaskListener)} if any
 * {@link DiamondJoinTrigger} found on project, add a {@link DiamondJoinAction}
 * on build. {@link #onCompleted(AbstractBuild, TaskListener)} if any
 * {@link DiamondJoinAction} found, notify build completion to
 * {@link DiamondJoinAction} and trigger Join proejcts build if needed.
 * 
 * <p>
 * Listener is always Hudson-wide, so once registered it gets notifications for
 * every build that happens in this Hudson.
 * 
 * @author Julien Bouyoud
 */
@Extension
public class DiamondRunListener
        extends
        RunListener<AbstractBuild<? extends AbstractProject<?, ?>, ? extends AbstractBuild<?, ?>>> {
	
	/** Class Logger */
	private static final Logger LOGGER = Logger
	        .getLogger(DiamondRunListener.class.getName());
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.listeners.RunListener#onStarted(hudson.model.Run,
	 * hudson.model.TaskListener)
	 */
	@Override
	public void onStarted(
	        final AbstractBuild<? extends AbstractProject<?, ?>, ? extends AbstractBuild<?, ?>> build,
	        final TaskListener listener) {
		if (build == null) {
			throw new IllegalArgumentException("build == null");
		}
		if (listener == null) {
			throw new IllegalArgumentException("listener == null");
		}
		super.onStarted(build, listener);
		
		// If build was start by upstream project dependency
		if (build.getCause(UpstreamCause.class) != null) {
			// Search on all direct Projects hierarchy an diamondTrigger
			final DiamondJoinTrigger diamondTrigger = build.getProject()
			        .getPublishersList().get(DiamondJoinTrigger.class);
			// if any diamondTrigger was found on project direct hierarchy
			if (diamondTrigger != null) {
				// Search All Upstream Join Build dependencies
				final List<DiamondJoinAction> joinActions = findJoinActions(build);
				// Add new Join Action on Build
				final DiamondJoinAction joinAction = new DiamondJoinAction(
				        build, diamondTrigger);
				joinAction.addJoinDependencies(joinActions);
				build.addAction(joinAction);
				
				if (LOGGER.isLoggable(Level.FINE)) {
					LOGGER.fine(joinAction.toString());
				}
			}
		} else {
			// User or other automatic Trigger a new Build.
			// For all Project Upstream hierarchy rebuild dependency Tree.
			buildJoinActionDependencyTree(build, build.getProject(), true);
		}
	}
	
	private List<DiamondJoinAction> buildJoinActionDependencyTree(
	        final AbstractBuild<? extends AbstractProject<?, ?>, ? extends AbstractBuild<?, ?>> masterJoinBuild,
	        final AbstractProject<?, ?> project, final boolean isRootProject) {
		if (masterJoinBuild == null) {
			throw new IllegalArgumentException("masterJoinBuild == null");
		}
		if (project == null) {
			throw new IllegalArgumentException("project == null");
		}
		final List<DiamondJoinAction> joinActions = new ArrayList<DiamondJoinAction>();
		// Just get the first Join Publisher that we found.
		// Here i dunno how to solve cross-diamonds.
		final DiamondJoinTrigger diamondTrigger = project.getPublishersList()
		        .get(DiamondJoinTrigger.class);
		
		DiamondJoinAction projectJoinAction = null;
		if (diamondTrigger != null
		        && (isRootProject || diamondTrigger
		                .isEvenIfBuildStartedOnDownstream())) {
			// Build join Action
			final DiamondJoinAction tmpJoinAction = new DiamondJoinAction(
			        masterJoinBuild, diamondTrigger);
			// Check if this join action aleady exist and if it no points it
			// self.
			if (!tmpJoinAction.pointsItself()
			        && !masterJoinBuild.getActions().contains(tmpJoinAction)) {
				projectJoinAction = tmpJoinAction;
				// Add only if build doesn't contains the same joinAction
				masterJoinBuild.addAction(projectJoinAction);
				joinActions.add(projectJoinAction);
				if (LOGGER.isLoggable(Level.FINE)) {
					LOGGER.fine(projectJoinAction.toString());
				}
			}
		}
		
		// If we are allowed to search in upstream dependency graph, do it
		if (diamondTrigger != null
		        && diamondTrigger.isEvenIfBuildStartedOnDownstream()
		        || diamondTrigger == null) {
			// Search in upper Project Hierarchy
			for (final AbstractProject<?, ?> upstreamProject : project
			        .getUpstreamProjects()) {
				// Recursive call
				final List<DiamondJoinAction> upstreamJoinActions = buildJoinActionDependencyTree(
				        masterJoinBuild, upstreamProject, false);
				joinActions.addAll(upstreamJoinActions);
				// For current JoinAction add all new upstream Dependencies
				if (projectJoinAction != null) {
					projectJoinAction.addJoinDependencies(upstreamJoinActions);
				}
			}
		}
		return joinActions;
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.listeners.RunListener#onCompleted(hudson.model.Run,
	 * hudson.model.TaskListener)
	 */
	@Override
	public void onCompleted(
	        final AbstractBuild<? extends AbstractProject<?, ?>, ? extends AbstractBuild<?, ?>> build,
	        final TaskListener listener) {
		if (build == null) {
			throw new IllegalArgumentException("build == null");
		}
		if (listener == null) {
			throw new IllegalArgumentException("listener == null");
		}
		super.onCompleted(build, listener);
		//
		for (final DiamondJoinAction joinAction : findJoinActions(build)) {
			// Notify downstream build of masterJoin Action of build
			// completion
			joinAction.notifyBuildCompletion(build, listener);
		}
	}
	
	/**
	 * Search all {@link DiamondJoinAction} on Build hierarchy through
	 * {@link UpstreamCause}
	 * 
	 * @param build
	 *            the child where the {@link DiamondJoinAction} will be search
	 *            in.
	 * @return list of all found {@link DiamondJoinAction} on build hierarchy
	 */
	@SuppressWarnings("rawtypes")
	private List<DiamondJoinAction> findJoinActions(
	        final AbstractBuild<? extends AbstractProject<?, ?>, ? extends AbstractBuild<?, ?>> build) {
		if (build == null) {
			throw new IllegalArgumentException("build == null");
		}
		final List<DiamondJoinAction> joinActions = new ArrayList<DiamondJoinAction>();
		// Scan Upstream build causes
		for (final Cause cause : build.getCauses()) {
			if (cause instanceof UpstreamCause) {
				final UpstreamCause upstreamCause = (UpstreamCause) cause;
				
				final AbstractProject project = Hudson.getInstance()
				        .getItemByFullName(upstreamCause.getUpstreamProject(),
				                AbstractProject.class);
				if (project != null) {
					final Run run = project.getBuildByNumber(upstreamCause
					        .getUpstreamBuild());
					if (run instanceof AbstractBuild) {
						joinActions
						        .addAll(findJoinActions((AbstractBuild<?, ?>) run));
					}
				}
			}
		}
		// Add only not completed Join Action on build
		for (final DiamondJoinAction availableJoinActionOnBuild : build
		        .getActions(DiamondJoinAction.class)) {
			if (!availableJoinActionOnBuild.isCompleted()) {
				joinActions.add(availableJoinActionOnBuild);
			}
		}
		return joinActions;
	}
	
}
