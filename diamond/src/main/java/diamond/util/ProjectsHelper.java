package diamond.util;

import hudson.model.Items;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.plugins.downstream_ext.DownstreamTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.tasks.BuildTrigger;

import java.util.LinkedList;
import java.util.List;

import diamond.DiamondJoinTrigger;

/**
 * Utility Class around {@link AbstractProject}. This utility class helps
 * resolve Project dependency arround all Hudson plugins.
 * <p>
 * Managed Relationship :
 * <ul>
 * <li>Hudson Normal Relationship</li>
 * <li>"downstream-ext" Relationships</li>
 * <li>"parameterized-trigger" Relationships</li>
 * <li>Diamond Join Relationships</li>
 * </ul>
 * 
 * @author Julien Bouyoud
 */
public final class ProjectsHelper {
	
	/**
	 * Private Constructor for Utility Class
	 */
	private ProjectsHelper() {
		// No Op
	}
	
	/**
	 * Find all downstream Projects for each managed relationship plugins
	 * 
	 * @param project
	 *            root project where hierarchy will be computed
	 * @param onlyDirectChildrens
	 *            limit downstream projects to non recursive hierarchy
	 * @param resolveJoinDependencies
	 *            flag indicates if diamond join dependencies should be computed
	 * @return a list of all downstream projects
	 */
	public static final List<AbstractProject<?, ?>> getDownstreamProjectsHierarchy(
	        final AbstractProject<?, ?> project,
	        final boolean onlyDirectChildrens,
	        final boolean resolveJoinDependencies) {
		if (project == null) {
			throw new IllegalArgumentException("project == null");
		}
		return getAllDownstreamProjectHierarchy(project, onlyDirectChildrens,
		        resolveJoinDependencies);
	}
	
	/**
	 * Recursive method to find all downstream Projects for each managed
	 * relationship plugins
	 * 
	 * @param project
	 *            root project where hierarchy will be computed
	 * @param onlyDirectChildrens
	 *            limit downstream projects to non recursive hierarchy
	 * @param resolveJoinDependencies
	 *            flag indicates if diamond join dependencies should be computed
	 * @return a list of all downstream projects
	 */
	private static List<AbstractProject<?, ?>> getAllDownstreamProjectHierarchy(
	        final AbstractProject<?, ?> project,
	        final boolean onlyDirectChildrens,
	        final boolean resolveJoinDependencies) {
		if (project == null) {
			throw new IllegalArgumentException("project == null");
		}
		final List<AbstractProject<?, ?>> downstreamProjects = new LinkedList<AbstractProject<?, ?>>();
		// Build "normal" Hierarchy
		final BuildTrigger buildTrigger = project.getPublishersList().get(
		        BuildTrigger.class);
		if (buildTrigger != null) {
			for (final AbstractProject<?, ?> childProject : buildTrigger
			        .getChildProjects()) {
				if (!downstreamProjects.contains(childProject)) {
					downstreamProjects.add(childProject);
				}
				if (!onlyDirectChildrens) {
					downstreamProjects.addAll(getAllDownstreamProjectHierarchy(
					        childProject, false, resolveJoinDependencies));
				}
			}
		}
		// Build downstream-ext Plugin Hierarchy
		if (Hudson.getInstance().getPlugin("downstream-ext") != null) {
			final DownstreamTrigger downstreamTrigger = project
			        .getPublishersList().get(DownstreamTrigger.class);
			if (downstreamTrigger != null) {
				for (final AbstractProject<?, ?> childProject : downstreamTrigger
				        .getChildProjects()) {
					if (!downstreamProjects.contains(childProject)) {
						downstreamProjects.add(childProject);
					}
					if (!onlyDirectChildrens) {
						downstreamProjects
						        .addAll(getAllDownstreamProjectHierarchy(
						                childProject, false,
						                resolveJoinDependencies));
					}
				}
			}
		}
		// Build parameterized-trigger
		if (Hudson.getInstance().getPlugin("parameterized-trigger") != null) {
			final hudson.plugins.parameterizedtrigger.BuildTrigger parametizedBuildTrigger = project
			        .getPublishersList()
			        .get(hudson.plugins.parameterizedtrigger.BuildTrigger.class);
			if (parametizedBuildTrigger != null) {
				for (final BuildTriggerConfig config : parametizedBuildTrigger
				        .getConfigs()) {
					for (final AbstractProject<?, ?> childProject : Items
					        .fromNameList(config.getProjects(),
					                AbstractProject.class)) {
						if (!downstreamProjects.contains(childProject)) {
							downstreamProjects.add(childProject);
						}
						if (!onlyDirectChildrens) {
							downstreamProjects
							        .addAll(getAllDownstreamProjectHierarchy(
							                childProject, false,
							                resolveJoinDependencies));
						}
					}
				}
			}
		}
		// Build other join Hierarchy
		final DiamondJoinTrigger joinTrigger = project.getPublishersList().get(
		        DiamondJoinTrigger.class);
		if (joinTrigger != null && resolveJoinDependencies) {
			for (final AbstractProject<?, ?> childProject : joinTrigger
			        .getAllJoinProjects()) {
				if (!downstreamProjects.contains(childProject)) {
					downstreamProjects.add(childProject);
				}
				downstreamProjects.addAll(getAllDownstreamProjectHierarchy(
				        childProject, onlyDirectChildrens, true));
			}
		}
		
		return downstreamProjects;
	}
	
}
