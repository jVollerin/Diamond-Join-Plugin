package diamond;

import hudson.Extension;
import hudson.Launcher;
import hudson.Plugin;
import hudson.Util;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.Item;
import hudson.model.Items;
import hudson.model.Result;
import hudson.model.Saveable;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.listeners.ItemListener;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Messages;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.BuildTrigger;
import hudson.util.DescribableList;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import diamond.util.ProjectsHelper;

/**
 * Trigger Build of join projects after all downstream hierarchy was executed.
 * 
 * @author Julien Bouyoud
 * @see Recorder
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class DiamondJoinTrigger extends Recorder implements DependecyDeclarer {
	
	/** Class Logger */
	protected static final Logger LOGGER = Logger
	        .getLogger(DiamondJoinTrigger.class.getName());
	/**
	 * Comma-separated list of join projects to be scheduled.
	 */
	private String joinProjects;
	
	/**
	 * Threshold status to trigger join builds.
	 * 
	 * Default as "SUCCESS".
	 */
	private final Result threshold;
	
	/**
	 * Flag indicates if Downstream Build should trigger join and post-join
	 * Actions.
	 */
	private final boolean evenIfBuildStartedOnDownstream;
	
	/**
	 * List of all postJoin Actions. Presently only 2 kind of Publisher
	 * supported. See {@link DescriptorImpl#getApplicableDescriptors()} for more
	 * details.
	 */
	private final DescribableList<Publisher, Descriptor<Publisher>> postJoinActions;
	
	/**
	 * Construct a new {@link DiamondJoinTrigger}.
	 * 
	 * @param postJoinActionPublishers
	 *            list of all post-join actions Publishers
	 * @param joinProjects
	 *            Comma-separated list of join projects to be scheduled.
	 * @param evenIfDownstreamUnstable
	 *            flag indicates if Threshold status to trigger join builds must
	 *            be {@link Result#SUCCESS} or {@link Result#UNSTABLE}.
	 * @param evenIfBuildStartedOnDownstream
	 *            Flag indicates if Downstream Build should trigger join and
	 *            post-join Actions.
	 */
	@DataBoundConstructor
	public DiamondJoinTrigger(final List<Publisher> postJoinActionPublishers,
	        final String joinProjects, final boolean evenIfDownstreamUnstable,
	        final boolean evenIfBuildStartedOnDownstream) {
		if (postJoinActionPublishers == null) {
			throw new IllegalArgumentException(
			        "postJoinActionPublishers == null");
		}
		if (joinProjects == null) {
			throw new IllegalArgumentException("joinProjects == null");
		}
		this.joinProjects = joinProjects;
		this.evenIfBuildStartedOnDownstream = evenIfBuildStartedOnDownstream;
		threshold = evenIfDownstreamUnstable ? Result.UNSTABLE : Result.SUCCESS;
		postJoinActions = new DescribableList<Publisher, Descriptor<Publisher>>(
		        Saveable.NOOP, postJoinActionPublishers);
	}
	
	/**
	 * Return a flag that indicates if Downstream Build should trigger join and
	 * post-join Actions.
	 * 
	 * @return <code>true</code> if downstream project build should trigger join
	 *         and post-join actions, <code>false</code> else.
	 */
	public boolean isEvenIfBuildStartedOnDownstream() {
		return evenIfBuildStartedOnDownstream;
	}
	
	/**
	 * Return a comma-separated list of join projects to be scheduled.
	 * 
	 * @return a comma-separated list of join projects to be scheduled.
	 */
	public String getJoinProjectsValue() {
		return joinProjects;
	}
	
	/**
	 * Return a list of join {@link AbstractProject} to be scheduled.
	 * 
	 * @return list of join {@link AbstractProject} to be scheduled.
	 */
	public List<AbstractProject> getJoinProjects() {
		if (joinProjects == null || "".equals(joinProjects.trim())) {
			return Collections.emptyList();
		}
		return Items.fromNameList(joinProjects, AbstractProject.class);
	}
	
	/**
	 * Return threshold status to trigger join builds.
	 * 
	 * @return Threshold status to trigger join builds.
	 */
	public Result getThreshold() {
		return threshold;
	}
	
	/**
	 * Return a list of all postJoin Actions
	 * 
	 * @return List of all postJoin Actions
	 */
	public DescribableList<Publisher, Descriptor<Publisher>> getPostJoinActions() {
		return postJoinActions;
	}
	
	/**
	 * Return if Trigger contains any post-join actions.
	 * 
	 * @return <code>true</code> if this trigger contains any post-join actions,
	 *         <code>false</code> else.
	 */
	public boolean containsAnyPostBuildAction() {
		return postJoinActions != null && postJoinActions.size() > 0;
	}
	
	/**
	 * Compute Collection of all joined project through "project list" and post
	 * Join actions
	 * 
	 * @return Comma-separated list of all joined project through "project list"
	 *         and post Join actions
	 */
	public String getAllJoinProjectsValue() {
		return Items.toNameList(getAllJoinProjects());
	}
	
	/**
	 * Compute Collection of all joined project through "project list" and post
	 * Join actions
	 * 
	 * @return a Collection of all joined project through "project list" and
	 *         post Join actions
	 */
	public List<AbstractProject> getAllJoinProjects() {
		final List<AbstractProject> allJoinProjectsList = new ArrayList<AbstractProject>();
		allJoinProjectsList.addAll(getJoinProjects());
		if (Hudson.getInstance().getPlugin("parameterized-trigger") != null) {
			final hudson.plugins.parameterizedtrigger.BuildTrigger parametizedBuildTrigger = postJoinActions
			        .get(hudson.plugins.parameterizedtrigger.BuildTrigger.class);
			if (parametizedBuildTrigger != null) {
				for (final BuildTriggerConfig config : parametizedBuildTrigger
				        .getConfigs()) {
					for (final AbstractProject<?, ?> childProject : Items
					        .fromNameList(config.getProjects(),
					                AbstractProject.class)) {
						if (!allJoinProjectsList.contains(childProject)) {
							allJoinProjectsList.add(childProject);
						}
					}
				}
			}
		}
		return Collections.unmodifiableList(allJoinProjectsList);
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.tasks.BuildStep#getRequiredMonitorService()
	 */
	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.tasks.Publisher#needsToRunAfterFinalized()
	 */
	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see
	 * hudson.tasks.BuildStepCompatibilityLayer#prebuild(hudson.model.AbstractBuild
	 * , hudson.model.BuildListener)
	 */
	@Override
	public boolean prebuild(final AbstractBuild<?, ?> build,
	        final BuildListener listener) {
		// Do Not prebuild postJoinAction here...wait union of all subprojects.
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see
	 * hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild
	 * , hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public boolean perform(final AbstractBuild<?, ?> build,
	        final Launcher launcher, final BuildListener listener)
	        throws InterruptedException, IOException {
		// Do Not Run postJoinAction here...wait union of all subprojects.
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.DependecyDeclarer#buildDependencyGraph(hudson.model.
	 * AbstractProject, hudson.model.DependencyGraph)
	 */
	@Override
	public void buildDependencyGraph(final AbstractProject owner,
	        final DependencyGraph graph) {
		if (owner == null) {
			throw new IllegalArgumentException("owner == null");
		}
		if (graph == null) {
			throw new IllegalArgumentException("graph == null");
		}
		
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine(owner.getName()
			        + " ==> "
			        + Items.toNameList(ProjectsHelper
			                .getDownstreamProjectsHierarchy(owner, false, true)));
		}
		// Build Child Projects Dependencies
		buildChildDependencyGraph(owner, owner, graph, getAllJoinProjects());
	}
	
	/**
	 * Recursive method to compute dependency graph of all projects hierarchy
	 * 
	 * @param masterProject
	 *            project that contains {@link DiamondJoinTrigger} dependency
	 * @param project
	 *            project to inspect
	 * @param graph
	 *            dependency graph where {@link UnionDependency} added
	 * @param list
	 */
	private void buildChildDependencyGraph(
	        final AbstractProject<?, ?> masterProject,
	        final AbstractProject<?, ?> project, final DependencyGraph graph,
	        final List<AbstractProject> allJoinedProjects) {
		if (masterProject == null) {
			throw new IllegalArgumentException("masterProject == null");
		}
		if (project == null) {
			throw new IllegalArgumentException("project == null");
		}
		if (graph == null) {
			throw new IllegalArgumentException("graph == null");
		}
		if (allJoinedProjects == null) {
			throw new IllegalArgumentException("allJoinedProjects == null");
		}
		for (final AbstractProject<?, ?> childProject : ProjectsHelper
		        .getDownstreamProjectsHierarchy(project, true, true)) {
			buildChildDependencyGraph(masterProject, childProject, graph,
			        allJoinedProjects);
		}
		addUnionDependency(masterProject, project, graph, allJoinedProjects);
	}
	
	/**
	 * Add {@link UnionDependency} only if project parameters was not in all
	 * joined projects
	 * 
	 * @param masterProject
	 *            project that contains {@link DiamondJoinTrigger} dependency
	 * @param project
	 *            project where dependency is issued
	 * @param graph
	 *            dependency graph where {@link UnionDependency} added
	 */
	private void addUnionDependency(final AbstractProject<?, ?> masterProject,
	        final AbstractProject<?, ?> project, final DependencyGraph graph,
	        final List<AbstractProject> allJoinedProjects) {
		if (masterProject == null) {
			throw new IllegalArgumentException("masterProject == null");
		}
		if (project == null) {
			throw new IllegalArgumentException("project == null");
		}
		if (graph == null) {
			throw new IllegalArgumentException("graph == null");
		}
		if (allJoinedProjects == null) {
			throw new IllegalArgumentException("allJoinedProjects == null");
		}
		// Check if project not points to joined projects hierarchy
		for (final AbstractProject<?, ?> joinedProject : allJoinedProjects) {
			if (joinedProject.equals(project)) {
				return;
			}
			for (final AbstractProject<?, ?> downstreamJoinedProject : ProjectsHelper
			        .getDownstreamProjectsHierarchy(joinedProject, true, true)) {
				if (downstreamJoinedProject.equals(project)) {
					return;
				}
			}
		}
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("Add Dependency \t\t==>\t" + project.getName()
			        + " to all join Projects : "
			        + Items.toNameList(allJoinedProjects));
		}
		
		// Add all join Project to current project
		for (final AbstractProject<?, ?> joinProject : getJoinProjects()) {
			graph.addDependency(new UnionDependency(masterProject, project,
			        joinProject));
		}
		// Add all parameterized join projects if needed
		final Plugin parameterizedTrigger = Hudson.getInstance().getPlugin(
		        "parameterized-trigger");
		if (parameterizedTrigger != null) {
			for (final hudson.plugins.parameterizedtrigger.BuildTrigger buildTrigger : postJoinActions
			        .getAll(hudson.plugins.parameterizedtrigger.BuildTrigger.class)) {
				for (final BuildTriggerConfig buildConfig : buildTrigger
				        .getConfigs()) {
					for (final AbstractProject<?, ?> joinProject : Items
					        .fromNameList(buildConfig.getProjects(),
					                AbstractProject.class)) {
						graph.addDependency(new UnionDependency(masterProject,
						        project, joinProject));
					}
				}
			}
		}
	}
	
	/**
	 * Called from {@link ItemListenerImpl} when a job is renamed.
	 * <p>
	 * Copied from {@link BuildTrigger}
	 * 
	 * @param oldName
	 *            old Project name
	 * @param newName
	 *            new Project name
	 * @return true if this {@link DiamondJoinTrigger} is changed and needs to
	 *         be saved.
	 */
	public boolean onJobRenamed(final String oldName, final String newName) {
		// quick test
		if (!joinProjects.contains(oldName)) {
			return false;
		}
		boolean changed = false;
		
		// we need to do this per string, since old Project object is already
		// gone.
		final String[] projects = joinProjects.split(",");
		for (int i = 0; i < projects.length; i++) {
			if (projects[i].trim().equals(oldName)) {
				projects[i] = newName;
				changed = true;
			}
		}
		
		if (changed) {
			final StringBuilder b = new StringBuilder();
			for (final String p : projects) {
				if (b.length() > 0) {
					b.append(',');
				}
				b.append(p);
			}
			joinProjects = b.toString();
		}
		
		return changed;
	}
	
	/**
	 * {@link DiamondJoinTrigger} UI Descriptor.
	 */
	@Extension
	public static final class DescriptorImpl extends
	        BuildStepDescriptor<Publisher> {
		
		/*
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return "Diamond Join Trigger";
		}
		
		/*
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#getHelpFile()
		 */
		@Override
		public String getHelpFile() {
			return "/plugin/diamond/DiamondJoinTrigger/help.html";
		}
		
		/*
		 * (non-Javadoc)
		 * @see
		 * hudson.model.Descriptor#newInstance(org.kohsuke.stapler.StaplerRequest
		 * , net.sf.json.JSONObject)
		 */
		@Override
		public Publisher newInstance(final StaplerRequest req,
		        final JSONObject formData) throws FormException {
			if (req == null) {
				throw new IllegalArgumentException("req == null");
			}
			if (formData == null) {
				throw new IllegalArgumentException("formData == null");
			}
			
			// Rebuild triggers save (even though java doc says otherwise)
			// => first build list of Post Join Actions, then call constructor
			final List<Publisher> postJoinActions = new ArrayList<Publisher>();
			
			final JSONObject postJoinActionsValue = formData
			        .optJSONObject("postJoinActions");
			if (postJoinActionsValue != null) {
				final JSONObject postJoinPublishers = postJoinActionsValue
				        .optJSONObject("postJoinActions");
				if (postJoinPublishers != null) {
					for (final Descriptor<Publisher> applicablePublisherDescriptor : getApplicableDescriptors()) {
						if (postJoinPublishers
						        .has(applicablePublisherDescriptor
						                .getJsonSafeClassName())) {
							postJoinActions
							        .add(applicablePublisherDescriptor
							                .newInstance(
							                        req,
							                        postJoinPublishers
							                                .getJSONObject(applicablePublisherDescriptor
							                                        .getJsonSafeClassName())));
						}
					}
				}
			}
			// Fetch "normal" join projects list
			String childProjectsString = formData.getString("joinProjects")
			        .trim();
			if (childProjectsString.endsWith(",")) {
				childProjectsString = childProjectsString.substring(0,
				        childProjectsString.length() - 1).trim();
			}
			return new DiamondJoinTrigger(
			        postJoinActions,
			        childProjectsString,
			        formData.has("evenIfDownstreamUnstable")
			                && formData.getBoolean("evenIfDownstreamUnstable"),
			        formData.has("evenIfBuildStartedOnDownstream")
			                && formData
			                        .getBoolean("evenIfBuildStartedOnDownstream"));
		}
		
		/*
		 * (non-Javadoc)
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@Override
		public boolean isApplicable(final Class clazz) {
			// Diamond Trigger is always applicable
			return true;
		}
		
		/**
		 * Return a list of all supported {@link Publisher} for post-joi
		 * Actions.
		 * 
		 * <ul>
		 * <li>parameterized-trigger</li>
		 * <li>copyarchiver</li>
		 * </ul>
		 * 
		 * @return list of all supported {@link Publisher} for post-joi Actions
		 */
		public List<Descriptor<Publisher>> getApplicableDescriptors() {
			final ArrayList<Descriptor<Publisher>> list = new ArrayList<Descriptor<Publisher>>();
			final Plugin parameterizedTrigger = Hudson.getInstance().getPlugin(
			        "parameterized-trigger");
			if (parameterizedTrigger != null) {
				list.add(Hudson
				        .getInstance()
				        .getDescriptorByType(
				                hudson.plugins.parameterizedtrigger.BuildTrigger.DescriptorImpl.class));
			}
			final Plugin copyArchiver = Hudson.getInstance().getPlugin(
			        "copyarchiver");
			if (copyArchiver != null) {
				list.add(Hudson
				        .getInstance()
				        .getDescriptorByType(
				                com.thalesgroup.hudson.plugins.copyarchiver.CopyArchiverPublisher.CopyArchiverDescriptor.class));
			}
			return list;
		}
		
		/**
		 * JoinProjects field validation method.
		 * 
		 * Copied from hudson.tasks.BuildTrigger.doCheck(Item project, String
		 * value)
		 * 
		 * @param project
		 *            project container
		 * @param value
		 *            value of field JoinProjects
		 * @return {@link FormValidation} status of field JoinProjects.
		 */
		public FormValidation doCheckJoinProjects(
		        @AncestorInPath final Item project,
		        @QueryParameter final String value) {
			// Require CONFIGURE permission on this project
			if (!project.hasPermission(Item.CONFIGURE)) {
				return FormValidation.ok();
			}
			if (value == null || "".equals(value.trim())) {
				return FormValidation
				        .warning("No Join Actions will be performed");
			}
			
			final StringTokenizer tokens = new StringTokenizer(
			        Util.fixNull(value), ",");
			boolean hasProjects = false;
			while (tokens.hasMoreTokens()) {
				final String projectName = tokens.nextToken().trim();
				if (StringUtils.isNotBlank(projectName)) {
					final Item item = Hudson.getInstance().getItemByFullName(
					        projectName, Item.class);
					if (item == null) {
						return FormValidation.error(Messages
						        .BuildTrigger_NoSuchProject(projectName,
						                AbstractProject
						                        .findNearest(projectName)
						                        .getName()));
					}
					if (!(item instanceof AbstractProject)) {
						return FormValidation.error(Messages
						        .BuildTrigger_NotBuildable(projectName));
					}
					hasProjects = true;
				}
			}
			if (!hasProjects) {
				return FormValidation.error(Messages
				        .BuildTrigger_NoProjectSpecified());
			}
			return FormValidation.ok();
		}
		
		/**
		 * Autocompletion method for field JoinProjects
		 * 
		 * Copied from
		 * hudson.tasks.BuildTrigger.doAutoCompleteChildProjects(String value)
		 * 
		 * @param value
		 *            current value of field JoinProjects
		 * @return list of possible {@link AutoCompletionCandidates} for current
		 *         value.
		 */
		public AutoCompletionCandidates doAutoCompleteJoinProjects(
		        @QueryParameter final String value) {
			final AutoCompletionCandidates candidates = new AutoCompletionCandidates();
			final List<Job> jobs = Hudson.getInstance().getItems(Job.class);
			for (final Job<?, ?> job : jobs) {
				if (job.getFullName().startsWith(value)) {
					if (job.hasPermission(Item.READ)) {
						candidates.add(job.getFullName());
					}
				}
			}
			return candidates;
		}
	}
	
	/**
	 * Ajout d'un Extention pour suveiller le projet renomé, et maintenir les
	 * link des projets entre eux.
	 * 
	 * Copied from
	 * {@link hudson.tasks.BuildTrigger.DescriptorImpl.ItemListenerImpl}
	 */
	@Extension
	public static final class ItemListenerImpl extends ItemListener {
		
		/*
		 * (non-Javadoc)
		 * @see hudson.model.listeners.ItemListener#onRenamed(hudson.model.Item,
		 * java.lang.String, java.lang.String)
		 */
		@Override
		public void onRenamed(final Item item, final String oldName,
		        final String newName) {
			if (item == null) {
				throw new IllegalArgumentException("item == null");
			}
			if (oldName == null) {
				throw new IllegalArgumentException("oldName == null");
			}
			if (newName == null) {
				throw new IllegalArgumentException("newName == null");
			}
			// update DiamondTrigger of other projects that point to this
			// object. can't we generalize this?
			for (final Project<?, ?> p : Hudson.getInstance().getProjects()) {
				final DiamondJoinTrigger t = p.getPublishersList().get(
				        DiamondJoinTrigger.class);
				if (t != null) {
					if (t.onJobRenamed(oldName, newName)) {
						try {
							p.save();
						} catch (final IOException e) {
							LOGGER.log(Level.WARNING,
							        "Failed to persist project setting during rename from "
							                + oldName + " to " + newName, e);
						}
					}
				}
			}
		}
	}
	
}
