package com.asu.score.hackslash.statistics;

import java.io.IOException;
import java.util.*;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.gitective.core.CommitFinder;
import org.gitective.core.filter.commit.AuthorSetFilter;
import org.gitective.core.filter.commit.DiffLineCountFilter;
import org.gitective.core.stat.AuthorHistogramFilter;
import org.gitective.core.stat.CommitCalendar;
import org.gitective.core.stat.Month;
import org.gitective.core.stat.UserCommitActivity;
import org.gitective.core.stat.YearCommitActivity;

import com.asu.score.hackslash.model.ShowGitLogsModel;

public class GitData {
	private GitController gitCtrl = GitController.getInstance();

	public Set<String> getContributor() throws IOException {
		gitPull();
		Repository repo = gitCtrl.getRepo();
		// Git git = new Git(repo);
		AuthorSetFilter authors = new AuthorSetFilter();
		CommitFinder afinder = new CommitFinder(repo);
		afinder.setFilter(authors).find();
		Set<String> set = new HashSet<String>();
		for (PersonIdent author : authors.getPersons()) {
			set.add(author.getName());
			/* System.out.println(author); */
		}

		return set;
	}

	public Map<String, Integer> getCommitsPerContributor() throws IOException {
		gitPull();
		Repository repo = gitCtrl.getRepo();
		// Repository repo = new FileRepository(GIT_PATH);
		// Git git = new Git(repo);
		Map<String, Integer> map = new HashMap<String, Integer>();
		AuthorHistogramFilter afilter = new AuthorHistogramFilter();
		CommitFinder cfinder = new CommitFinder(repo);
		cfinder.setFilter(afilter).find();
		UserCommitActivity[] activity = afilter.getHistogram()
				.getUserActivity();

		for (UserCommitActivity user : activity) {
			map.put(user.getName(), user.getCount());
			System.out.println(user.getName() + " has done " + user.getCount()
					+ " commits.");

		}
		return map;
	}

	public List<List<String>> getMonthlyCommits() throws IOException {
		gitPull();
		Repository repo = gitCtrl.getRepo();
		// Repository repo = new FileRepository(GIT_PATH);
		// Git git = new Git(repo);
		List<List<String>> all = new ArrayList<List<String>>();

		AuthorHistogramFilter afilter = new AuthorHistogramFilter();
		CommitFinder cfinder = new CommitFinder(repo);
		cfinder.setFilter(afilter).find();
		UserCommitActivity[] activity = afilter.getHistogram()
				.getUserActivity();
		CommitCalendar commits = new CommitCalendar(activity);

		for (YearCommitActivity year : commits.getYears()) {
			int m = 0;
			for (int c : year.getMonths()) {
				String month = Month.month(m) + "-" + year.getYear();
				List<String> mo = new ArrayList<String>();
				mo.add(month);
				mo.add(String.valueOf(c));
				all.add(mo);
				// map.put(month,c);
				System.out.println(c + " commits in " + Month.month(m) + "-"
						+ year.getYear());
				m++;
			}
		}
		return all;
	}

	public List<ShowGitLogsModel> getGitCommitLog() throws IOException,
			GitAPIException {
		gitPull();
		Git git = gitCtrl.getGit();
		Repository repo = gitCtrl.getRepo();
		RevWalk walk = new RevWalk(repo);
		// List<List<String>> log = new ArrayList<List<String>>();
		List<ShowGitLogsModel> log = new ArrayList<ShowGitLogsModel>();
		// List<String> logRow = new ArrayList();
		List<String> logRow;
		ShowGitLogsModel showGitLogsModel;

		List<Ref> branches = git.branchList().call();
		for (Ref branch : branches) {
			String branchName = branch.getName();

			System.out.println("Commits of branch: " + branch.getName());
			System.out.println("-------------------------------------");

			Iterable<RevCommit> commits = git.log().all().call();

			for (RevCommit commit : commits) {
				boolean foundInThisBranch = false;

				RevCommit targetCommit = walk.parseCommit(repo.resolve(commit
						.getName()));
				for (Map.Entry<String, Ref> e : repo.getAllRefs().entrySet()) {
					if (e.getKey().startsWith(Constants.R_HEADS)) {
						if (walk.isMergedInto(targetCommit,
								walk.parseCommit(e.getValue().getObjectId()))) {
							String foundInBranch = e.getValue().getName();
							if (branchName.equals(foundInBranch)) {
								foundInThisBranch = true;
								break;
							}
						}
					}
				}

				if (foundInThisBranch) {
					showGitLogsModel = new ShowGitLogsModel();
					showGitLogsModel.setLogIdentifier(commit.getName());
					showGitLogsModel.setAuthorName(commit.getAuthorIdent()
							.getName());
					long time = commit.getCommitTime();
					time = time * 1000;
					showGitLogsModel.setDate(String.valueOf(new Date(time)));
					showGitLogsModel.setCommitMessage(commit.getFullMessage());
					// logRow = new ArrayList();
					// logRow.add(commit.getName());
					// logRow.add(commit.getAuthorIdent().getName());
					// logRow.add(String.valueOf(new
					// Date(commit.getCommitTime())));
					// logRow.add(commit.getFullMessage());
					// System.out.println(commit.getName());
					// System.out.println(commit.getAuthorIdent().getName());
					// System.out.println(new Date(commit.getCommitTime()));
					// System.out.println(commit.getFullMessage());
					log.add(showGitLogsModel);
				}

			}
		}
		return log;
	}

	public int getTotalCurrentMonthCommits() throws IOException {
		gitPull();
		Repository repo = gitCtrl.getRepo();
		AuthorHistogramFilter afilter = new AuthorHistogramFilter();
		CommitFinder cfinder = new CommitFinder(repo);
		cfinder.setFilter(afilter).find();
		UserCommitActivity[] activity = afilter.getHistogram()
				.getUserActivity();
		CommitCalendar commits = new CommitCalendar(activity);

		int c = commits.getMonthCount(Calendar.getInstance()
				.get(Calendar.MONTH));
		System.out.println(c);

		return c;
	}

	public int getTotalCommits() throws IOException {
		gitPull();
		Repository repo = gitCtrl.getRepo();
		AuthorHistogramFilter afilter = new AuthorHistogramFilter();
		CommitFinder cfinder = new CommitFinder(repo);
		cfinder.setFilter(afilter).find();
		UserCommitActivity[] activity = afilter.getHistogram()
				.getUserActivity();
		CommitCalendar commits = new CommitCalendar(activity);
		int count = 0;
		for (YearCommitActivity year : commits.getYears()) {
			int m = 0;
			for (int c : year.getMonths()) {
				count = count + c;
			}
		}
		return count;
	}

	public List<Integer> getLocChange() throws IOException {
		gitPull();
		Repository repo = gitCtrl.getRepo();
		CommitFinder finder = new CommitFinder(repo);
		DiffLineCountFilter filter = new DiffLineCountFilter();
		finder.setFilter(filter);
		finder.find();
		List<Integer> loc = new ArrayList<Integer>();
		loc.add((int) filter.getAdded());
		loc.add((int) filter.getEdited());
		loc.add((int) filter.getDeleted());
		loc.add((int) filter.getAdded() + (int) filter.getEdited());
		System.out.println("Added:\t" + filter.getAdded());
		System.out.println("Changed:\t" + filter.getEdited());
		System.out.println("Deleted:\t" + filter.getDeleted());
		System.out.println("CSI:\t" + (filter.getAdded() + filter.getEdited()));
		return loc;

	}

	private void gitPull() {
		try {
			gitCtrl.getGit().pull().call();
			System.out.println("pulled remote repository");
		} catch (GitAPIException e) {
			e.printStackTrace();
			System.out
					.println("Exception occured while pulling remote repository");
		}
	}

	public static void main(String[] args) throws IOException, GitAPIException {
		GitData git = new GitData();
		// Set<String> set = git.getContributor();
		// Map<String,Integer> map = git.getCommitsPerContributor();
		// List<List<String>> mo = git.getMonthlyCommits();
		// List<List<String>> log = git.getGitCommitLog();
		// int c = git.getTotalCurrentMonthCommits();
		// System.out.println(set);
		// System.out.println(log);
		// System.out.println(map2);
		// System.out.println(c);
	}
}
