package diamond;

import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause.UserCause;
import hudson.model.FreeStyleProject;

import java.util.ArrayList;
import java.util.List;

import org.jvnet.hudson.test.HudsonTestCase;

public abstract class DiamondJointestCase extends HudsonTestCase {
	protected FreeStyleProject masterProject;
	protected List<FreeStyleProject> joinProjects;
	
	public DiamondJointestCase() {
	}
	
	public DiamondJointestCase(final String name) {
		super(name);
	}
	
	public static void assertInSequence(final AbstractBuild<?, ?>... builds) {
		AbstractBuild<?, ?> previousBuild = null;
		for (final AbstractBuild<?, ?> build : builds) {
			if (previousBuild != null) {
				assertFinished(previousBuild).beforeStarted(build);
			}
			previousBuild = build;
		}
	}
	
	public static <ProjectT extends AbstractProject<ProjectT, BuildT>, BuildT extends AbstractBuild<ProjectT, BuildT>> void assertInSequence(
	        final AbstractBuild<?, ?> firstBuild,
	        final List<BuildT> intermediateBuilds, final List<BuildT> lastBuilds) {
		assertFinished(firstBuild).beforeStarted(intermediateBuilds);
		for (final BuildT lastBuild : lastBuilds) {
			assertStarted(lastBuild).afterFinished(intermediateBuilds);
		}
	}
	
	public static void assertNotBuilt(final AbstractProject<?, ?> project) {
		final List<?> builds = project.getBuilds();
		assertTrue("Project " + project + " should not have been built!",
		        builds.isEmpty());
	}
	
	public static <T extends AbstractProject<?, ?>> void assertNotBuilt(
	        final List<T> projects) {
		for (final AbstractProject<?, ?> project : projects) {
			assertNotBuilt(project);
		}
	}
	
	public static BuildTimeConstraint assertFinished(
	        final AbstractBuild<?, ?> build) {
		final long started = build.getTimestamp().getTimeInMillis();
		final long duration = build.getDuration();
		final long finished = started + duration;
		return new BuildTimeConstraint(build.toString(), "finished", finished);
	}
	
	public static BuildTimeConstraint assertStarted(
	        final AbstractBuild<?, ?> build) {
		final long started = build.getTimestamp().getTimeInMillis();
		return new BuildTimeConstraint(build.toString(), "started", started);
	}
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		masterProject = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("masterproject");
		joinProjects = new ArrayList<FreeStyleProject>();
		joinProjects.add(ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("join0"));
	}
	
	protected FreeStyleBuild build(final FreeStyleProject buildStartProject)
	        throws Exception {
		return build(buildStartProject, false);
	}
	
	protected FreeStyleBuild build(final FreeStyleProject buildStartProject,
	        final boolean allowTrigeringDownstreamJoin) throws Exception {
		ProjectsUtils
		        .addJoinTriggerTo(masterProject, allowTrigeringDownstreamJoin,
		                joinProjects.toArray(new FreeStyleProject[joinProjects
		                        .size()]));
		hudson.rebuildDependencyGraph();
		
		final FreeStyleBuild build = buildStartProject.scheduleBuild2(0,
		        new UserCause()).get();
		waitUntilNoActivity();
		return build;
	}
	
	public static class BuildTimeConstraint {
		private final long millis;
		private final String buildName;
		private final String state;
		
		BuildTimeConstraint(final String buildName, final String state,
		        final long millis) {
			this.millis = millis;
			this.buildName = buildName;
			this.state = state;
		}
		
		public void beforeStarted(final AbstractBuild<?, ?> build) {
			final long started = build.getTimestamp().getTimeInMillis();
			assertTrue(String.format("%s not %s before %s started!", buildName,
			        state, build.toString()), millis < started);
		}
		
		public <T extends AbstractBuild<?, ?>> void beforeStarted(
		        final List<T> builds) {
			for (final T build : builds) {
				beforeStarted(build);
			}
		}
		
		public void afterFinished(final AbstractBuild<?, ?> build) {
			final long started = build.getTimestamp().getTimeInMillis();
			final long duration = build.getDuration();
			final long finished = started + duration;
			assertTrue(String.format("%s not %s after %s finished!", buildName,
			        state, build.toString()), millis > finished);
		}
		
		public <T extends AbstractBuild<?, ?>> void afterFinished(
		        final List<T> builds) {
			for (final T build : builds) {
				afterFinished(build);
			}
		}
	}
	
}
