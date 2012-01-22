package diamond;

import hudson.model.Action;
import hudson.model.DependencyGraph;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.util.List;

/**
 * Represents an edge in the dependency graph for Join Build using
 * {@link DiamondJoinAction}.
 * 
 * @author Julien Bouyoud
 */
public class UnionDependency extends DependencyGraph.Dependency {
	
	/** Root project where Union Dependency is defined */
	private final AbstractProject<?, ?> masterProject;
	
	/**
	 * Construct a new {@link UnionDependency}
	 * 
	 * @param masterProject
	 *            project where Union Dependency is defined
	 * @param upstreamProject
	 *            a project that must be joined
	 * @param downstreamProject
	 *            a join project
	 */
	public UnionDependency(final AbstractProject<?, ?> masterProject,
	        final AbstractProject<?, ?> upstreamProject,
	        final AbstractProject<?, ?> downstreamProject) {
		super(upstreamProject, downstreamProject);
		if (masterProject == null) {
			throw new IllegalArgumentException("masterProject == null");
		}
		this.masterProject = masterProject;
	}
	
	/*
	 * (non-Javadoc)
	 * @see
	 * hudson.model.DependencyGraph.Dependency#shouldTriggerBuild(hudson.model
	 * .AbstractBuild, hudson.model.TaskListener, java.util.List)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public boolean shouldTriggerBuild(final AbstractBuild build,
	        final TaskListener listener, final List<Action> actions) {
		// As default build process do not run UnionDependency
		return false;
	}
	
	/**
	 * Return the root project where Union Dependency is defined
	 * 
	 * @return the root project where Union Dependency is defined
	 */
	public AbstractProject<?, ?> getMasterProject() {
		return masterProject;
	}
	
}
