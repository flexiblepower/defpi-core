/// **
// * File XjcCompiler.java
// *
// * Copyright 2017 TNO
// */
// package org.flexiblepower.plugin.servicegen.compiler;
//
// import java.io.ByteArrayOutputStream;
// import java.io.IOException;
// import java.io.OutputStream;
// import java.io.PrintStream;
// import java.nio.file.Files;
// import java.nio.file.Path;
//
// import org.flexiblepower.codegen.compiler.Compiler;
//
// import com.sun.tools.xjc.Driver;
//
// import lombok.Setter;
//
/// **
// * XjcCompiler
// *
// * @author coenvl
// * @version 0.1
// * @since Jun 28, 2017
// */
// public class XjcCompiler implements Compiler {
//
// @Setter
// private String basePackageName = "";
//
// /*
// * (non-Javadoc)
// *
// * @see org.flexiblepower.plugin.servicegen.Compiler#compileSources(java.nio.file.Path, java.nio.file.Path)
// */
// @Override
// public void compile(final Path filePath, final Path targetPath) throws IOException {
// // Delay making the target folder to this point so it won't be made unnessecarily
// if (!targetPath.toFile().exists()) {
// Files.createDirectory(targetPath);
// }
//
// // Build and execute the command
// @SuppressWarnings("unused")
// final Driver d = new Driver();
//
// try (
// final OutputStream stateOs = new ByteArrayOutputStream();
// final PrintStream statePs = new PrintStream(stateOs);
// final OutputStream outOs = new ByteArrayOutputStream();
// final PrintStream outPs = new PrintStream(outOs)) {
// final String[] args = {"-npa", "-d", targetPath.toString(), "-p", XjcCompiler.this.basePackageName,
// filePath.toString(), "-Xfluent-api"};
// final int result = Driver.run(args, statePs, outPs);
//
// if (result != 0) {
// throw new IOException(
// "Exception while compiling " + filePath + ":\n" + stateOs.toString() + "\n" + outOs.toString());
// }
// } catch (final Exception e) {
// throw new IOException("Exception while compiling " + filePath, e);
// }
// }
//
// }
