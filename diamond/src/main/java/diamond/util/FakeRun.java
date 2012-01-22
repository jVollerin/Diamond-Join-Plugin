package diamond.util;

import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Run;

import java.io.IOException;
import java.util.List;

/**
 * Class to override global run result due to assert in {@link Run}. Act as
 * Proxy.
 * 
 * @param <P>
 * @param <R>
 * @see AbstractBuild
 */
public final class FakeRun<P extends AbstractProject<P, R>, R extends AbstractBuild<P, R>>
        extends AbstractBuild<P, R> {
	
	/** original build */
	private final AbstractBuild<P, R> originalBuild;
	/** Computed result */
	private final Result globalResult;
	
	/**
	 * Create new Fake Run
	 * 
	 * @param build
	 *            original build
	 * @param globalResult
	 *            computed union result
	 * @throws IOException
	 *             if any error occured
	 */
	public FakeRun(final AbstractBuild<P, R> build, final Result globalResult)
	        throws IOException {
		super(build.getProject(), build.getTimestamp());
		this.originalBuild = build;
		this.globalResult = globalResult;
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.Run#getResult()
	 */
	@Override
	public Result getResult() {
		return globalResult;
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.AbstractBuild#run()
	 */
	@Override
	public void run() {
		throw new UnsupportedOperationException("Build aleady run.");
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.Run#getCauses()
	 */
	@Override
	public List<Cause> getCauses() {
		return originalBuild.getCauses();
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.AbstractBuild#getParent()
	 */
	@Override
	public P getParent() {
		return originalBuild.getParent();
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.Run#getNumber()
	 */
	@Override
	public int getNumber() {
		return originalBuild.getNumber();
	}
}
