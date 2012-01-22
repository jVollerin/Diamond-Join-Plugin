package diamond;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

public class NestedDiamondsTest extends DiamondJointestCase {
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1 (as master 1, join to inter2)<br/>
	 *  			-> inter1.1<br/>
	 *  			-> inter1.2<br/>
	 *  		   inter2<br/>
	 *  			-> inter2.1<br/>
	 *  			-> inter2.2<br/>
	 *  					-> join0
	 *  </code>
	 */
	public void testTwoNestedDiamond() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 2);
		final FreeStyleProject inter2 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter2");
		final List<FreeStyleProject> inters1X = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 2);
		final List<FreeStyleProject> inters2X = ProjectsUtils
		        .createFreeStyleProjects("inter2.", 2);
		
		ProjectsUtils.addChildsProjectTo(inters.get(1), inters1X);
		ProjectsUtils.addChildsProjectTo(inter2, inters2X);
		ProjectsUtils.addJoinTriggerTo(inters.get(1), true, inter2);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		// Test1 : Build start on masterProject
		assertInSequence(build(masterProject, true),
		        ProjectsUtils.getUniqueBuilds(CollectionUtils.union(
		                CollectionUtils.union(
		                        CollectionUtils.union(inters,
		                                Collections.singletonList(inter2)),
		                        inters1X), inters2X)),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0 (build start here)<br/>
	 *  		-> inter1 (as master 1, join to inter2)<br/>
	 *  			-> inter1.1<br/>
	 *  			-> inter1.2<br/>
	 *  		   inter2<br/>
	 *  			-> inter2.1<br/>
	 *  			-> inter2.2<br/>
	 *  					-> join0
	 *  </code>
	 */
	public void testTwoNestedDiamond2() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 2);
		final FreeStyleProject inter2 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter2");
		final List<FreeStyleProject> inters1X = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 2);
		final List<FreeStyleProject> inters2X = ProjectsUtils
		        .createFreeStyleProjects("inter2.", 2);
		
		ProjectsUtils.addChildsProjectTo(inters.get(1), inters1X);
		ProjectsUtils.addChildsProjectTo(inter2, inters2X);
		ProjectsUtils.addJoinTriggerTo(inters.get(1), true, inter2);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		// Test1 : Build start on inter0
		final FreeStyleBuild inter0Build = build(inters.get(0), true);
		assertNotBuilt(masterProject);
		assertNotBuilt(inters.get(1));
		assertNotBuilt(inter2);
		assertNotBuilt(inters1X);
		assertNotBuilt(inters2X);
		
		assertInSequence(inter0Build, Collections.<FreeStyleBuild> emptyList(),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1 (as master 1, join to inter2)<br/>
	 *  			-> inter1.1<br/>
	 *  			-> inter1.2 (build start here)<br/>
	 *  		   inter2<br/>
	 *  			-> inter2.1<br/>
	 *  			-> inter2.2<br/>
	 *  					-> join0
	 *  </code>
	 */
	public void testTwoNestedDiamond3() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 2);
		final FreeStyleProject inter2 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter2");
		final List<FreeStyleProject> inters1X = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 2);
		final List<FreeStyleProject> inters2X = ProjectsUtils
		        .createFreeStyleProjects("inter2.", 2);
		
		ProjectsUtils.addChildsProjectTo(inters.get(1), inters1X);
		ProjectsUtils.addChildsProjectTo(inter2, inters2X);
		ProjectsUtils.addJoinTriggerTo(inters.get(1), true, inter2);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		// Test1 : Build start on inter1.1
		final FreeStyleBuild inter11Build = build(inters1X.get(1), true);
		assertNotBuilt(masterProject);
		assertNotBuilt(inters);
		assertNotBuilt(inters1X.get(0));
		assertInSequence(inter11Build,
		        ProjectsUtils.getUniqueBuilds(CollectionUtils.union(inters2X,
		                Collections.singletonList(inter2))),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1 (as master 1, join to inter2)<br/>
	 *  			-> inter1.1<br/>
	 *  			-> inter1.2<br/>
	 *  		   inter2<br/>
	 *  			-> inter2.1<br/>
	 *  			-> inter2.2 (build start here)<br/>
	 *  					-> join0
	 *  </code>
	 */
	public void testTwoNestedDiamond4() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 2);
		final FreeStyleProject inter2 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter2");
		final List<FreeStyleProject> inters1X = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 2);
		final List<FreeStyleProject> inters2X = ProjectsUtils
		        .createFreeStyleProjects("inter2.", 2);
		
		ProjectsUtils.addChildsProjectTo(inters.get(1), inters1X);
		ProjectsUtils.addChildsProjectTo(inter2, inters2X);
		ProjectsUtils.addJoinTriggerTo(inters.get(1), true, inter2);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		// Test1 : Build start on inter2.2
		final FreeStyleBuild inter11Build = build(inters2X.get(1), true);
		assertNotBuilt(masterProject);
		assertNotBuilt(inters);
		assertNotBuilt(inters1X);
		assertNotBuilt(inter2);
		assertNotBuilt(inters2X.get(0));
		assertInSequence(inter11Build,
		        Collections.<FreeStyleBuild> emptyList(),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1 (as master 1, join to inter2)<br/>
	 *  			-> inter1.1<br/>
	 *  			-> inter1.2<br/>
	 *  		   inter2 (as master 2, join to inter3)<br/>
	 *  			-> inter2.1<br/>
	 *  			-> inter2.2<br/>
	 *  		-> inter3 (as master 3, join to inter4)<br/>
	 *  			-> inter3.1<br/>
	 *  			-> inter3.2<br/>
	 *  		-> inter4<br/>
	 *  			-> inter4.1<br/>
	 *  			-> inter4.2<br/>
	 *  					-> join0
	 *  </code>
	 */
	public void testManyNestedDiamond() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 2);
		final FreeStyleProject inter2 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter2");
		final FreeStyleProject inter3 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter3");
		final FreeStyleProject inter4 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter4");
		final List<FreeStyleProject> inters1X = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 2);
		final List<FreeStyleProject> inters2X = ProjectsUtils
		        .createFreeStyleProjects("inter2.", 2);
		final List<FreeStyleProject> inters3X = ProjectsUtils
		        .createFreeStyleProjects("inter3.", 2);
		final List<FreeStyleProject> inters4X = ProjectsUtils
		        .createFreeStyleProjects("inter4.", 2);
		
		ProjectsUtils.addChildsProjectTo(inters.get(1), inters1X);
		ProjectsUtils.addChildsProjectTo(inter2, inters2X);
		ProjectsUtils.addChildsProjectTo(inter3, inters3X);
		ProjectsUtils.addChildsProjectTo(inter4, inters4X);
		ProjectsUtils.addJoinTriggerTo(inters.get(1), true, inter2);
		ProjectsUtils.addJoinTriggerTo(inter2, true, inter3);
		ProjectsUtils.addJoinTriggerTo(inter3, true, inter4);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		// Test : Build start on masterProject
		assertInSequence(build(masterProject, true),
		        ProjectsUtils.getUniqueBuilds(CollectionUtils.union(inters,
		                CollectionUtils.union(Arrays.asList(inter2, inter3,
		                        inter4), CollectionUtils.union(inters1X,
		                        CollectionUtils.union(inters2X, CollectionUtils
		                                .union(inters3X, inters4X)))))),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1 (as master 1, join to inter2)<br/>
	 *  			-> inter1.1<br/>
	 *  			-> inter1.2<br/>
	 *  		   inter2 (as master 2, join to inter3)<br/>
	 *  			-> inter2.1<br/>
	 *  			-> inter2.2 (build start here)<br/>
	 *  		-> inter3 (as master 3, join to inter4)<br/>
	 *  			-> inter3.1<br/>
	 *  			-> inter3.2<br/>
	 *  		-> inter4<br/>
	 *  			-> inter4.1<br/>
	 *  			-> inter4.2<br/>
	 *  					-> join0
	 *  </code>
	 */
	public void testManyNestedDiamond2() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 2);
		final FreeStyleProject inter2 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter2");
		final FreeStyleProject inter3 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter3");
		final FreeStyleProject inter4 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter4");
		final List<FreeStyleProject> inters1X = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 2);
		final List<FreeStyleProject> inters2X = ProjectsUtils
		        .createFreeStyleProjects("inter2.", 2);
		final List<FreeStyleProject> inters3X = ProjectsUtils
		        .createFreeStyleProjects("inter3.", 2);
		final List<FreeStyleProject> inters4X = ProjectsUtils
		        .createFreeStyleProjects("inter4.", 2);
		
		ProjectsUtils.addChildsProjectTo(inters.get(1), inters1X);
		ProjectsUtils.addChildsProjectTo(inter2, inters2X);
		ProjectsUtils.addChildsProjectTo(inter3, inters3X);
		ProjectsUtils.addChildsProjectTo(inter4, inters4X);
		ProjectsUtils.addJoinTriggerTo(inters.get(1), true, inter2);
		ProjectsUtils.addJoinTriggerTo(inter2, true, inter3);
		ProjectsUtils.addJoinTriggerTo(inter3, true, inter4);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		// Test1 : Build start on inter2.2
		final FreeStyleBuild inter22Build = build(inters2X.get(1), true);
		assertNotBuilt(masterProject);
		assertNotBuilt(inters);
		assertNotBuilt(inters1X);
		assertNotBuilt(inter2);
		assertNotBuilt(inters2X.get(0));
		
		assertInSequence(
		        inter22Build,
		        ProjectsUtils.getUniqueBuilds(CollectionUtils.union(
		                Arrays.asList(inter3, inter4),
		                CollectionUtils.union(inters3X, inters4X))),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
}
