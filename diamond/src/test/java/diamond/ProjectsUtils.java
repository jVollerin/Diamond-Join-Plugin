package diamond;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.plugins.downstream_ext.DownstreamTrigger;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.BuildTrigger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;

public class ProjectsUtils {
	
	public static FreeStyleProject createFreeStyleProjectWithNoQuietPeriod(
	        final String name) throws Exception {
		if (name == null) {
			throw new IllegalArgumentException("name == null");
		}
		final FreeStyleProject freestyleProject = Hudson.getInstance()
		        .createProject(FreeStyleProject.class, name);
		freestyleProject.setQuietPeriod(0);
		return freestyleProject;
	}
	
	public static FreeStyleProject createFailingFreeStyleProject()
	        throws Exception {
		final FreeStyleProject project = createFreeStyleProjectWithNoQuietPeriod("Failing");
		project.getPublishersList().add(ResultSetter.FAILURE());
		return project;
	}
	
	public static FreeStyleProject createUnstableFreeStyleProject()
	        throws Exception {
		final FreeStyleProject project = createFreeStyleProjectWithNoQuietPeriod("Unstable");
		project.getPublishersList().add(ResultSetter.UNSTABLE());
		return project;
	}
	
	public static List<FreeStyleProject> createFreeStyleProjects(
	        final String prefix, final int number) throws Exception {
		if (prefix == null) {
			throw new IllegalArgumentException("prefix == null");
		}
		final List<FreeStyleProject> createdProjects = new ArrayList<FreeStyleProject>();
		for (int i = 0; i < number; i++) {
			createdProjects.add(createFreeStyleProjectWithNoQuietPeriod(prefix
			        + i));
		}
		return createdProjects;
	}
	
	public static void addChildsProjectTo(
	        final AbstractProject<?, ?> rootProject,
	        final AbstractProject<?, ?> projectToAdd) throws Exception {
		if (projectToAdd == null) {
			throw new IllegalArgumentException("projectToAdd == null");
		}
		if (rootProject == null) {
			throw new IllegalArgumentException("rootProject == null");
		}
		addChildsProjectTo(rootProject, projectToAdd.getName());
	}
	
	public static void addChildsProjectTo(
	        final AbstractProject<?, ?> rootProject,
	        final String projectNameToAdd) throws Exception {
		if (rootProject == null) {
			throw new IllegalArgumentException("rootProject == null");
		}
		if (projectNameToAdd == null) {
			throw new IllegalArgumentException("projectNameToAdd == null");
		}
		rootProject.getPublishersList().add(
		        new BuildTrigger(projectNameToAdd, false));
	}
	
	public static <T extends AbstractProject<?, ?>> void addChildsProjectTo(
	        final AbstractProject<?, ?> rootProject,
	        final AbstractProject<?, ?>... childs) throws Exception {
		final List<String> projects = new ArrayList<String>(childs.length);
		for (final AbstractProject<?, ?> project : childs) {
			projects.add(project.getName());
		}
		addChildsProjectTo(rootProject, StringUtils.join(projects, ","));
	}
	
	public static <T extends AbstractProject<?, ?>> void addChildsProjectTo(
	        final AbstractProject<?, ?> rootProject, final List<T> projectsToAdd)
	        throws Exception {
		final List<String> projects = new ArrayList<String>(
		        projectsToAdd.size());
		for (final AbstractProject<?, ?> project : projectsToAdd) {
			projects.add(project.getName());
		}
		addChildsProjectTo(rootProject, StringUtils.join(projects, ","));
	}
	
	public static <T extends AbstractProject<?, ?>> void addDownstreamExtChildsProjectTo(
	        final T rootProject, final List<T> projectsToAdd) throws Exception {
		final List<String> projects = new ArrayList<String>(
		        projectsToAdd.size());
		for (final AbstractProject<?, ?> project : projectsToAdd) {
			projects.add(project.getName());
		}
		addDownstreamExtChildsProjectTo(rootProject,
		        StringUtils.join(projects, ","));
	}
	
	public static void addDownstreamExtChildsProjectTo(
	        final AbstractProject<?, ?> rootProject,
	        final String projectNameToAdd) throws Exception {
		if (rootProject == null) {
			throw new IllegalArgumentException("rootProject == null");
		}
		if (projectNameToAdd == null) {
			throw new IllegalArgumentException("projectNameToAdd == null");
		}
		rootProject.getPublishersList().add(
		        new DownstreamTrigger(projectNameToAdd, Result.SUCCESS, false,
		                DownstreamTrigger.Strategy.AND_LOWER, null));
	}
	
	public static void addJoinTriggerTo(
	        final AbstractProject<?, ?> rootProject,
	        final AbstractProject<?, ?>... joinProjects) throws Exception {
		addJoinTriggerTo(rootProject, false, joinProjects);
	}
	
	public static void addJoinTriggerTo(
	        final AbstractProject<?, ?> rootProject,
	        final boolean allowTrigeringDownstreamJoin,
	        final AbstractProject<?, ?>... joinProjects) throws Exception {
		if (rootProject == null) {
			throw new IllegalArgumentException("rootProject == null");
		}
		if (joinProjects == null) {
			throw new IllegalArgumentException("joinProjects == null");
		}
		final List<String> projects = new ArrayList<String>(joinProjects.length);
		for (final AbstractProject<?, ?> project : joinProjects) {
			projects.add(project.getName());
		}
		rootProject.getPublishersList().add(
		        new DiamondJoinTrigger(Collections.<Publisher> emptyList(),
		                StringUtils.join(projects, ","), false,
		                allowTrigeringDownstreamJoin));
	}
	
	public static void addParameterizedJoinTriggerToProject(
	        final AbstractProject<?, ?> splitProject,
	        final AbstractProject<?, ?> joinProject,
	        final AbstractBuildParameters... params) throws Exception {
		addParameterizedJoinTriggerToProject(splitProject, joinProject,
		        ResultCondition.SUCCESS, params);
	}
	
	public static void addParameterizedJoinTriggerToProject(
	        final AbstractProject<?, ?> splitProject,
	        final AbstractProject<?, ?> joinProject,
	        final ResultCondition condition,
	        final AbstractBuildParameters... params) throws Exception {
		final BuildTriggerConfig config = new hudson.plugins.parameterizedtrigger.BuildTriggerConfig(
		        joinProject.getName(), condition, params);
		splitProject
		        .getPublishersList()
		        .add(new DiamondJoinTrigger(
		                Collections
		                        .<Publisher> singletonList(new hudson.plugins.parameterizedtrigger.BuildTrigger(
		                                config)), "", false, false));
	}
	
	public static <ProjectT extends AbstractProject<ProjectT, BuildT>, BuildT extends AbstractBuild<ProjectT, BuildT>> List<BuildT> getUniqueBuilds(
	        final Collection<ProjectT> projects) {
		final List<BuildT> buildList = new ArrayList<BuildT>();
		for (final AbstractProject<ProjectT, BuildT> project : projects) {
			buildList.add(getUniqueBuild(project));
		}
		return buildList;
	}
	
	public static <ProjectT extends AbstractProject<ProjectT, BuildT>, BuildT extends AbstractBuild<ProjectT, BuildT>> List<BuildT> getLastBuilds(
	        final List<ProjectT> projects) {
		final List<BuildT> buildList = new ArrayList<BuildT>();
		for (final AbstractProject<ProjectT, BuildT> project : projects) {
			buildList.add(getLastBuild(project));
		}
		return buildList;
	}
	
	public static <ProjectT extends AbstractProject<ProjectT, BuildT>, BuildT extends AbstractBuild<ProjectT, BuildT>> BuildT getUniqueBuild(
	        final AbstractProject<ProjectT, BuildT> project) {
		final List<BuildT> builds = project.getBuilds();
		Assert.assertTrue("Project " + project
		        + " should have been built exactly once but was triggered "
		        + builds.size() + " times!", builds.size() == 1);
		final BuildT build = builds.get(0);
		return build;
	}
	
	public static <ProjectT extends AbstractProject<ProjectT, BuildT>, BuildT extends AbstractBuild<ProjectT, BuildT>> BuildT getLastBuild(
	        final AbstractProject<ProjectT, BuildT> project) {
		final BuildT build = project.getLastBuild();
		Assert.assertNotNull(
		        "Project "
		                + project
		                + " should have been built at least once but was not triggered at all!",
		        build);
		return build;
	}
	
	public static class ResultSetter extends Notifier {
		private final Result result;
		
		public ResultSetter() {
			result = Result.FAILURE;
		}
		
		public ResultSetter(final Result result) {
			this.result = result;
		}
		
		public static ResultSetter FAILURE() {
			return new ResultSetter(Result.FAILURE);
		}
		
		public static ResultSetter UNSTABLE() {
			return new ResultSetter(Result.UNSTABLE);
		}
		
		@Extension
		public static class DescriptorImpl extends
		        BuildStepDescriptor<Publisher> {
			
			@Override
			public boolean isApplicable(
			        final Class<? extends AbstractProject> jobType) {
				return true;
			}
			
			@Override
			public String getDisplayName() {
				return "FailPublisher";
			}
		}
		
		@Override
		public BuildStepMonitor getRequiredMonitorService() {
			return BuildStepMonitor.NONE;
		}
		
		@Override
		public boolean perform(final AbstractBuild<?, ?> build,
		        final Launcher launcher, final BuildListener listener)
		        throws InterruptedException, IOException {
			build.setResult(result);
			return true;
		}
	}
	
}
