package diamond;

import hudson.matrix.MatrixProject;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.ExtractResourceSCM;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

@RunWith(Parameterized.class)
public class DiamondJoinManyConfigurationTest extends DiamondJointestCase {
	
	public static Map<Class<? extends AbstractProject<?, ?>>, Function<DiamondJoinManyConfigurationTest, AbstractProject<?, ?>>> projectType2Supplier = //
	ImmutableMap
	        .<Class<? extends AbstractProject<?, ?>>, Function<DiamondJoinManyConfigurationTest, AbstractProject<?, ?>>> of(
	                FreeStyleProject.class,
	                new Function<DiamondJoinManyConfigurationTest, AbstractProject<?, ?>>() {
		                @Override
		                public AbstractProject<?, ?> apply(
		                        final DiamondJoinManyConfigurationTest from) {
			                try {
				                return ProjectsUtils
				                        .createFreeStyleProjectWithNoQuietPeriod("int0"
				                                + Math.random());
			                } catch (final Exception e) {
				                e.printStackTrace();
				                fail(e.getMessage());
			                }
			                return null;
		                }
	                },
	                MavenModuleSet.class,
	                new Function<DiamondJoinManyConfigurationTest, AbstractProject<?, ?>>() {
		                @Override
		                public AbstractProject<?, ?> apply(
		                        final DiamondJoinManyConfigurationTest from) {
			                try {
				                final MavenModuleSet mavenProject = from
				                        .createMavenProject();
				                mavenProject.setQuietPeriod(0);
				                mavenProject.setScm(new ExtractResourceSCM(
				                        getClass().getResource(
				                                "maven-empty-mod.zip")));
				                
				                return mavenProject;
			                } catch (final Exception e) {
				                e.printStackTrace();
				                fail(e.getMessage());
			                }
			                return null;
		                }
	                },
	                MatrixProject.class,
	                new Function<DiamondJoinManyConfigurationTest, AbstractProject<?, ?>>() {
		                @Override
		                public AbstractProject<?, ?> apply(
		                        final DiamondJoinManyConfigurationTest from) {
			                try {
				                final MatrixProject matrixProject = from
				                        .createMatrixProject();
				                matrixProject.setQuietPeriod(0);
				                return matrixProject;
			                } catch (final Exception e) {
				                e.printStackTrace();
				                fail(e.getMessage());
			                }
			                return null;
		                }
	                });
	
	private final Class<?> splitProjClass;
	private final Class<?> intProjClass;
	private final Class<?> joinProjClass;
	
	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		final List<Object[]> parameters = new ArrayList<Object[]>();
		for (final Class<? extends AbstractProject<?, ?>> splitProjClass : projectType2Supplier
		        .keySet()) {
			for (final Class<? extends AbstractProject<?, ?>> intProjClass : projectType2Supplier
			        .keySet()) {
				for (final Class<? extends AbstractProject<?, ?>> joinProjClass : projectType2Supplier
				        .keySet()) {
					parameters.add(new Class<?>[] { splitProjClass,
					        intProjClass, joinProjClass });
				}
			}
		}
		return parameters;
	}
	
	public DiamondJoinManyConfigurationTest(final Class<?> splitProjClass,
	        final Class<?> intProjClass, final Class<?> joinProjClass) {
		super(DiamondJoinManyConfigurationTest.class.getName());
		if (joinProjClass == null) {
			throw new IllegalArgumentException("joinProjClass == null");
		}
		if (intProjClass == null) {
			throw new IllegalArgumentException("intProjClass == null");
		}
		if (splitProjClass == null) {
			throw new IllegalArgumentException("splitProjClass == null");
		}
		this.splitProjClass = splitProjClass;
		this.intProjClass = intProjClass;
		this.joinProjClass = joinProjClass;
	}
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		System.out.println("Starting test with parameters :"
		        + Joiner.on(", ").join(splitProjClass, intProjClass,
		                joinProjClass));
	}
	
	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();    // To change body of overridden methods use File |
		                  // Settings | File Templates.
	}
	
	@Test
	public void testJoinProjectShouldBeTriggered() throws Exception {
		configureDefaultMaven();
		final AbstractProject<?, ?> masterProject = projectType2Supplier.get(
		        splitProjClass).apply(this);
		final AbstractProject<?, ?> intProject = projectType2Supplier.get(
		        intProjClass).apply(this);
		final AbstractProject<?, ?> joinProject = projectType2Supplier.get(
		        joinProjClass).apply(this);
		
		ProjectsUtils.addChildsProjectTo(masterProject, intProject);
		ProjectsUtils.addJoinTriggerTo(masterProject, joinProject);
		Hudson.getInstance().rebuildDependencyGraph();
		
		final AbstractBuild<?, ?> masterProjectBuild = masterProject
		        .scheduleBuild2(0, new Cause.UserCause()).get();
		
		waitUntilNoActivityUpTo(120 * 1000);
		final AbstractBuild<?, ?> intBuild = ProjectsUtils
		        .getUniqueBuild(intProject);
		final AbstractBuild<?, ?> joinBuild = ProjectsUtils
		        .getUniqueBuild(joinProject);
		assertBuildStatusSuccess(intBuild);
		assertBuildStatusSuccess(joinBuild);
		assertInSequence(masterProjectBuild,
		        ProjectsUtils.getUniqueBuilds(Collections
		                .<AbstractProject> singletonList(intProject)),
		        ProjectsUtils.getUniqueBuilds(Collections
		                .<AbstractProject> singletonList(joinProject)));
	}
}
