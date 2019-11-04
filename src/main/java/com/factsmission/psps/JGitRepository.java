/*
 * The MIT License
 *
 * Copyright 2019 me.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.factsmission.psps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Ref;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 *
 * @author me
 */
public class JGitRepository implements Repository {

    private final static Path baseCheckout = Paths.get(System.getProperty("user.dir"), "checkout");
    private final String repoUri;
    private final String userName;
    private Git git;
    private final Path workingDir;
    private final String repoName;

    JGitRepository(String repository, String token) throws IOException {
        this.repoName = repository;
        this.repoUri = "https://github.com/" + repository + ".git";
        this.userName = token;
        workingDir = baseCheckout.resolve(repository);
        if (Files.exists(workingDir)) {
            Files.walk(workingDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete); //TODO recycle
        }
        try {
            git = Git.cloneRepository()
                    .setURI(repoUri)
                    .setDirectory(workingDir.toFile())
                    .setCloneAllBranches(false)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                    .call();
           
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        } finally {
            git.close();
        }
    }

    @Override
    public String[] getBranches() throws IOException {
        try {
            final List<Ref> branchRefs = git.branchList().setListMode(ListMode.REMOTE).call();
            final String[] result = new String[branchRefs.size()];
            int i = 0;
            for (Ref branchRef : branchRefs) {
                String[] parts = branchRef.getName().split("/");
                result[i++] = parts[parts.length-1];
            }
            return result;
        } catch (GitAPIException ex) {
            throw new IOException(ex);
        }
        
    }

    @Override
    public String getCommitURL() throws IOException {
        //something like https://github.com/factsmission/staging-website/commit/4467dc315fd80d4b98ad669e601b614d96e23041
        try {
            ObjectId id = git.getRepository().resolve(Constants.HEAD);
            String hexString = ObjectId.toString(id);
            return "https://github.com/"+repoName+"/commit/"+hexString;
        } catch (IncorrectObjectTypeException | RevisionSyntaxException ex) {
            throw new IOException(ex);
        } 
    }

    @Override
    public byte[] getContent(String repoPath) throws IOException {
        Path path = workingDir.resolve(repoPath);
        if (Files.isDirectory(path)) {
            return null;
        }
        return Files.readAllBytes(path);
    }

    @Override
    public Iterable<String> getPaths() throws IOException {
        return (Iterable<String>)Files.walk(workingDir)
            .sorted(Comparator.reverseOrder())
            .map(p -> {System.out.println(p); System.out.println(workingDir.relativize(p)); return workingDir.relativize(p);})
            .map(Path::toString)
            .filter(s -> !s.startsWith(".git"))::iterator;
    }

    @Override
    public void useBranch(String branch) throws IOException {
        try {
            git.checkout().setName("remotes/origin/"+branch).call();
        } catch (GitAPIException ex) {
            throw new IOException(ex);
        }
    }

}
