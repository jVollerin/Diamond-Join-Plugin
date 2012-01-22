package diamond;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

public class DiamondJoinTest extends DiamondJointestCase {
	
	public void testBasicValidation() throws IOException {
		final FreeStyleProject project = createFreeStyleProject("First");
		createFreeStyleProject("Second");
		final DiamondJoinTrigger.DescriptorImpl joinTriggerDescriptor = new DiamondJoinTrigger.DescriptorImpl();
		
		FormValidation formValidation = joinTriggerDescriptor
		        .doCheckJoinProjects(project, "");
		assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
		
		formValidation = joinTriggerDescriptor.doCheckJoinProjects(project,
		        null);
		assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
		
		formValidation = joinTriggerDescriptor.doCheckJoinProjects(project,
		        "First, Second");
		assertEquals(FormValidation.Kind.OK, formValidation.kind);
		
		formValidation = joinTriggerDescriptor.doCheckJoinProjects(project,
		        "First, Second, ");
		assertEquals(FormValidation.Kind.OK, formValidation.kind);
		
		formValidation = joinTriggerDescriptor.doCheckJoinProjects(project,
		        "First, Second,");
		assertEquals(FormValidation.Kind.OK, formValidation.kind);
		
		formValidation = joinTriggerDescriptor.doCheckJoinProjects(project,
		        "First, ,Second,");
		assertEquals(FormValidation.Kind.OK, formValidation.kind);
		
		formValidation = joinTriggerDescriptor.doCheckJoinProjects(project,
		        " ,First,Second,");
		assertEquals(FormValidation.Kind.OK, formValidation.kind);
		
		formValidation = joinTriggerDescriptor.doCheckJoinProjects(project,
		        "   First,Second,");
		assertEquals(FormValidation.Kind.OK, formValidation.kind);
		
		formValidation = joinTriggerDescriptor.doCheckJoinProjects(project,
		        "First, Third,Second,");
		assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
	}
	
	/**
	 * <code>master -> join
	 * </code>
	 */
	public void testWithoutInter() throws Exception {
		
		final FreeStyleBuild masterBuild = build(masterProject);
		final List<FreeStyleBuild> joinBuilds = ProjectsUtils
		        .getUniqueBuilds(joinProjects);
		for (final FreeStyleBuild joinBuild : joinBuilds) {
			assertInSequence(masterBuild, joinBuild);
		}
	}
	
	/**
	 * <code>master -> inter0 <br/>
	 * 						-> join
	 * </code>
	 * 
	 */
	public void testWithOneInter() throws Exception {
		final FreeStyleProject inter = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter0");
		ProjectsUtils.addChildsProjectTo(masterProject, inter);
		
		assertInSequence(build(masterProject),
		        ProjectsUtils.getUniqueBuilds(Arrays.asList(inter)),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1<br/>
	 *  					 -> join
	 * </code>
	 */
	public void testWithTwoInter() throws Exception {
		final FreeStyleProject inter = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter0");
		final FreeStyleProject inter1 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter1");
		ProjectsUtils.addChildsProjectTo(masterProject, inter, inter1);
		
		assertInSequence(build(masterProject),
		        ProjectsUtils.getUniqueBuilds(Arrays.asList(inter, inter1)),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1<br/>
	 *  		-> ...(inter20)<br/>
	 *  					 -> join
	 *  </code>
	 */
	public void testWithXInter() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 20);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		assertInSequence(build(masterProject),
		        ProjectsUtils.getUniqueBuilds(inters),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>master -> inter0 <br/>
	 * 						-> join0
	 * 						-> join1
	 * 						-> join2
	 * </code>
	 */
	public void testWithOneInterManyJoins() throws Exception {
		joinProjects.add(ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("join1"));
		joinProjects.add(ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("join2"));
		
		final FreeStyleProject inter = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter0");
		ProjectsUtils.addChildsProjectTo(masterProject, inter);
		
		assertInSequence(build(masterProject),
		        ProjectsUtils.getUniqueBuilds(Arrays.asList(inter)),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1<br/>
	 *  					-> join0
	 * 						-> join1
	 * 						-> join2
	 * </code>
	 */
	public void testWithTwoInterManyJoins() throws Exception {
		joinProjects.add(ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("join1"));
		joinProjects.add(ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("join2"));
		
		final FreeStyleProject inter = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter0");
		final FreeStyleProject inter1 = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("inter1");
		ProjectsUtils.addChildsProjectTo(masterProject, inter, inter1);
		
		assertInSequence(build(masterProject),
		        ProjectsUtils.getUniqueBuilds(Arrays.asList(inter, inter1)),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1<br/>
	 *  		-> ...(inter20)<br/>
	 *  					-> join0
	 * 						-> join1
	 * 						-> join2
	 *  </code>
	 */
	public void testWithXInterManyJoins() throws Exception {
		joinProjects.add(ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("join1"));
		joinProjects.add(ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("join2"));
		
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 20);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		assertInSequence(build(masterProject),
		        ProjectsUtils.getUniqueBuilds(inters),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  			-> inter0.1<br/>
	 *  			-> inter0.2<br/>
	 *  			-> inter0.3<br/>
	 *  		-> inter1<br/>
	 *  		-> inter2<br/>
	 *  		-> inter3<br/>
	 *  			-> inter3.1<br/>  
	 *  			-> inter3.2<br/>
	 *  			-> inter3.3<br/>
	 *  		-> inter4<br/>
	 *  					-> join0
	 *  </code>
	 */
	public void testWith2LevelOfHierarchy() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 5);
		final List<FreeStyleProject> intersoX = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 3);
		final List<FreeStyleProject> inters3X = ProjectsUtils
		        .createFreeStyleProjects("inter3.", 3);
		
		ProjectsUtils.addChildsProjectTo(inters.get(0), intersoX);
		ProjectsUtils.addChildsProjectTo(inters.get(3), inters3X);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		assertInSequence(build(masterProject),
		        ProjectsUtils.getUniqueBuilds(CollectionUtils.union(inters,
		                CollectionUtils.union(intersoX, inters3X))),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  			-> inter0.1<br/>
	 *  			-> inter0.2<br/>
	 *  			-> inter0.3<br/>
	 *  		-> inter1<br/>
	 *  		-> inter2<br/>
	 *  			-> inter2.1<br/>  
	 *  				-> inter2.1.1<br/>
	 *  				-> inter2.1.2<br/>   
	 *  				-> inter2.1.3<br/>
	 *  				-> inter2.1.4<br/>  
	 *  			-> inter2.2<br/>
	 *  			-> inter2.3<br/>
	 *  		-> inter3<br/>
	 *  			-> inter3.1<br/>  
	 *  			-> inter3.2<br/>
	 *  			-> inter3.3<br/>
	 *  		-> inter4<br/>
	 *  					-> join0
	 *  </code>
	 */
	public void testWith3LevelOfHierarchy() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 5);
		final List<FreeStyleProject> intersoX = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 3);
		final List<FreeStyleProject> inters2X = ProjectsUtils
		        .createFreeStyleProjects("inter2.", 3);
		final List<FreeStyleProject> inters21X = ProjectsUtils
		        .createFreeStyleProjects("inter2.1.", 4);
		final List<FreeStyleProject> inters3X = ProjectsUtils
		        .createFreeStyleProjects("inter3.", 3);
		
		ProjectsUtils.addChildsProjectTo(inters.get(0), intersoX);
		ProjectsUtils.addChildsProjectTo(inters.get(2), inters2X);
		ProjectsUtils.addChildsProjectTo(inters2X.get(0), inters21X);
		ProjectsUtils.addChildsProjectTo(inters.get(3), inters3X);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		assertInSequence(build(masterProject),
		        ProjectsUtils.getUniqueBuilds(CollectionUtils.union(inters,
		                CollectionUtils.union(CollectionUtils.union(intersoX,
		                        CollectionUtils.union(inters2X, inters21X)),
		                        inters3X))),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1 (unstable)<br/>
	 *  		-> inter2<br/>
	 *  					 -> join
	 * </code>
	 */
	public void testForOneProjectUnstable() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 2);
		inters.add(1, ProjectsUtils.createUnstableFreeStyleProject());
		
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		final FreeStyleBuild masterBuild = build(masterProject);
		
		assertFinished(masterBuild).beforeStarted(
		        ProjectsUtils.getUniqueBuilds(inters));
		assertNotBuilt(joinProjects);
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1 (falling)<br/>
	 *  		-> inter2<br/>
	 *  					 -> join
	 * </code>
	 */
	public void testForOneProjectFalling() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 2);
		inters.add(1, ProjectsUtils.createFailingFreeStyleProject());
		
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		final FreeStyleBuild masterBuild = build(masterProject);
		
		assertFinished(masterBuild).beforeStarted(
		        ProjectsUtils.getUniqueBuilds(inters));
		assertNotBuilt(joinProjects);
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  		-> inter1 (disabled)<br/>
	 *  		-> inter2<br/>
	 *  					 -> join
	 * </code>
	 */
	public void testIntermediateProjectDisabled() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 2);
		final FreeStyleProject disabledProject = ProjectsUtils
		        .createFreeStyleProjectWithNoQuietPeriod("disabled");
		disabledProject.disable();
		inters.add(1, disabledProject);
		
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		final FreeStyleBuild masterBuild = build(masterProject);
		assertNotBuilt(disabledProject);
		inters.remove(disabledProject);
		assertInSequence(masterBuild, ProjectsUtils.getUniqueBuilds(inters),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
		inters.add(disabledProject);
		
		// ReablaedIt
		disabledProject.enable();
		final FreeStyleBuild masterBuild2 = build(masterProject);
		assertInSequence(masterBuild2, ProjectsUtils.getLastBuilds(inters),
		        ProjectsUtils.getLastBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  			-> inter0.1<br/>
	 *  			-> inter0.2<br/>
	 *  			-> inter0.3<br/>
	 *  		-> inter1<br/>
	 *  		-> inter2 (build start here)<br/>
	 *  			-> inter2.1<br/>  
	 *  				-> inter2.1.1<br/>
	 *  				-> inter2.1.2<br/>   
	 *  				-> inter2.1.3<br/>
	 *  				-> inter2.1.4<br/>  
	 *  			-> inter2.2<br/>
	 *  			-> inter2.3<br/>
	 *  		-> inter3<br/>
	 *  			-> inter3.1<br/>  
	 *  			-> inter3.2<br/>
	 *  			-> inter3.3<br/>
	 *  		-> inter4<br/>
	 *  					-> join0
	 *  					-> join1
	 *  </code>
	 */
	public void testRunNonSplitProjectWithFunctionnalityDisabled()
	        throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 5);
		final List<FreeStyleProject> intersoX = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 3);
		final List<FreeStyleProject> inters2X = ProjectsUtils
		        .createFreeStyleProjects("inter2.", 3);
		final List<FreeStyleProject> inters21X = ProjectsUtils
		        .createFreeStyleProjects("inter2.1.", 4);
		final List<FreeStyleProject> inters3X = ProjectsUtils
		        .createFreeStyleProjects("inter3.", 3);
		
		ProjectsUtils.addChildsProjectTo(inters.get(0), intersoX);
		ProjectsUtils.addChildsProjectTo(inters.get(2), inters2X);
		ProjectsUtils.addChildsProjectTo(inters2X.get(0), inters21X);
		ProjectsUtils.addChildsProjectTo(inters.get(3), inters3X);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		final FreeStyleBuild build = build(inters.get(2));
		assertNotBuilt(intersoX);
		assertNotBuilt(inters3X);
		assertNotBuilt(joinProjects);
		assertInSequence(build, ProjectsUtils.getUniqueBuilds(CollectionUtils
		        .union(inters2X, inters21X)),
		        Collections.<FreeStyleBuild> emptyList());
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  			-> inter0.1<br/>
	 *  			-> inter0.2<br/>
	 *  			-> inter0.3<br/>
	 *  		-> inter1<br/>
	 *  		-> inter2 (build start here)<br/>
	 *  			-> inter2.1<br/>  
	 *  				-> inter2.1.1<br/>
	 *  				-> inter2.1.2<br/>   
	 *  				-> inter2.1.3<br/>
	 *  				-> inter2.1.4<br/>  
	 *  			-> inter2.2<br/>
	 *  			-> inter2.3<br/>
	 *  		-> inter3<br/>
	 *  			-> inter3.1<br/>  
	 *  			-> inter3.2<br/>
	 *  			-> inter3.3<br/>
	 *  		-> inter4<br/>
	 *  					-> join0
	 *  					-> join1
	 *  </code>
	 */
	public void testRunNonSplitProjectWithFunctionnalityEnabled()
	        throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 5);
		final List<FreeStyleProject> intersoX = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 3);
		final List<FreeStyleProject> inters2X = ProjectsUtils
		        .createFreeStyleProjects("inter2.", 3);
		final List<FreeStyleProject> inters21X = ProjectsUtils
		        .createFreeStyleProjects("inter2.1.", 4);
		final List<FreeStyleProject> inters3X = ProjectsUtils
		        .createFreeStyleProjects("inter3.", 3);
		
		ProjectsUtils.addChildsProjectTo(inters.get(0), intersoX);
		ProjectsUtils.addChildsProjectTo(inters.get(2), inters2X);
		ProjectsUtils.addChildsProjectTo(inters2X.get(0), inters21X);
		ProjectsUtils.addChildsProjectTo(inters.get(3), inters3X);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		assertInSequence(build(inters.get(2), true),
		        ProjectsUtils.getUniqueBuilds(CollectionUtils.union(inters2X,
		                inters21X)),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	public void testSlave() throws Exception {
		// Do it manually due to tearDown error ... dunno why
		// final List<FreeStyleProject> inter = ProjectsUtils
		// .createFreeStyleProjects("inter", 2);
		//
		// final FreeStyleProject slaveProject = ProjectsUtils
		// .createFreeStyleProjectWithNoQuietPeriod("slaved");
		// final DumbSlave slave = createOnlineSlave();
		// slaveProject.setAssignedNode(slave);
		//
		// final ArrayList<FreeStyleProject> allIntermediateProjects = new
		// ArrayList<FreeStyleProject>();
		// allIntermediateProjects.addAll(inter);
		// allIntermediateProjects.add(slaveProject);
		//
		// ProjectsUtils
		// .addChildsProjectTo(masterProject, allIntermediateProjects);
		//
		// final FreeStyleBuild masterBuild = build(masterProject);
		// assertInSequence(masterBuild, ProjectsUtils.getUniqueBuilds(inter),
		// ProjectsUtils.getUniqueBuilds(joinProjects));
		// assertInSequence(masterBuild,
		// ProjectsUtils.getUniqueBuilds(Arrays.asList(slaveProject)),
		// ProjectsUtils.getUniqueBuilds(joinProjects));
		//
		// assertEquals(slave, ProjectsUtils.getUniqueBuild(slaveProject)
		// .getBuiltOn());
		
	}
	
	/**
	 * <code>
	 *  master (downstream Ext)	-> inter0 (downstream Ext)<br/>
	 *  			-> inter0.1<br/>
	 *  			-> inter0.2<br/>
	 *  			-> inter0.3<br/>
	 *  		-> inter1<br/>
	 *  					-> join0
	 *  					-> join1
	 *  </code>
	 */
	public void testDownstreamExtTrigger() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 5);
		final List<FreeStyleProject> intersoX = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 3);
		
		ProjectsUtils.addDownstreamExtChildsProjectTo(inters.get(0), intersoX);
		ProjectsUtils.addDownstreamExtChildsProjectTo(masterProject, inters);
		
		assertInSequence(build(masterProject),
		        ProjectsUtils.getUniqueBuilds(inters),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
		assertInSequence(ProjectsUtils.getUniqueBuild(inters.get(0)),
		        ProjectsUtils.getUniqueBuilds(intersoX),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
	/**
	 * <code>
	 *  master 	-> inter0<br/>
	 *  			-> inter0.1<br/>
	 *  			-> inter0.2<br/>
	 *  			-> inter0.3<br/>
	 *  		-> inter1<br/>
	 *  (downstream Ext)-> inter2 (downstream Ext)<br/>
	 *  			-> inter2.1 (downstream Ext)<br/>  
	 *  				-> inter2.1.1<br/>
	 *  				-> inter2.1.2<br/>   
	 *  				-> inter2.1.3<br/>
	 *  				-> inter2.1.4<br/>  
	 *  			-> inter2.2<br/>
	 *  			-> inter2.3<br/>
	 *  		-> inter3<br/>
	 *  			-> inter3.1<br/>  
	 *  			-> inter3.2<br/>
	 *  			-> inter3.3<br/>
	 *  		-> inter4<br/>
	 *  					-> join0
	 *  					-> join1
	 *  </code>
	 */
	public void testMixedWithDownstreamExtTrigger() throws Exception {
		final List<FreeStyleProject> inters = ProjectsUtils
		        .createFreeStyleProjects("inter", 5);
		final List<FreeStyleProject> intersoX = ProjectsUtils
		        .createFreeStyleProjects("inter0.", 3);
		final List<FreeStyleProject> inters2X = ProjectsUtils
		        .createFreeStyleProjects("inter2.", 3);
		final List<FreeStyleProject> inters21X = ProjectsUtils
		        .createFreeStyleProjects("inter2.1.", 4);
		final List<FreeStyleProject> inters3X = ProjectsUtils
		        .createFreeStyleProjects("inter3.", 3);
		
		ProjectsUtils.addChildsProjectTo(inters.get(0), intersoX);
		ProjectsUtils.addDownstreamExtChildsProjectTo(inters.get(2), inters2X);
		ProjectsUtils.addDownstreamExtChildsProjectTo(inters2X.get(0),
		        inters21X);
		ProjectsUtils.addChildsProjectTo(inters.get(3), inters3X);
		ProjectsUtils.addChildsProjectTo(masterProject, inters);
		
		assertInSequence(build(masterProject),
		        ProjectsUtils.getUniqueBuilds(CollectionUtils.union(inters,
		                CollectionUtils.union(CollectionUtils.union(intersoX,
		                        CollectionUtils.union(inters2X, inters21X)),
		                        inters3X))),
		        ProjectsUtils.getUniqueBuilds(joinProjects));
	}
	
}
