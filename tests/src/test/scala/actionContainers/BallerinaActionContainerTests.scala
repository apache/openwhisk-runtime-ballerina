/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package actionContainers

import java.lang.Exception

import actionContainers.ActionContainer.withContainer
import common.WskActorSystem
import java.nio.file.{Files, Paths}
import java.util.Base64

import org.ballerinalang.compiler.CompilerPhase
import org.ballerinalang.compiler.CompilerOptionName.{COMPILER_PHASE, OFFLINE, PROJECT_DIR}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.wso2.ballerinalang.compiler.Compiler
import org.wso2.ballerinalang.compiler.util.{CompilerContext, CompilerOptions}
import org.wso2.ballerinalang.compiler.util.diagnotic.BLangDiagnosticLog
import spray.json._

@RunWith(classOf[JUnitRunner])
class BallerinaActionContainerTests extends BasicActionRunnerTests with WskActorSystem {

  lazy val ballerinaContainerImageName = "action-ballerina-v0.990.2"

  override def withActionContainer(env: Map[String, String] = Map.empty)(code: ActionContainer => Unit) = {
    withContainer(ballerinaContainerImageName, env)(code)
  }

  override val testNoSourceOrExec = {
    TestConfig("")
  }

  override val testInitCannotBeCalledMoreThanOnce = {
    TestConfig(buildBal("hello"))
  }

  override val testNotReturningJson = {
    TestConfig(buildBal("notjson"), enforceEmptyOutputStream = false)
  }

  override val testEnv = {
    TestConfig(buildBal("envparams"), enforceEmptyOutputStream = false, enforceEmptyErrorStream = false)
  }

  override val testEcho = {
    // see https://github.com/ballerina-platform/ballerina-lang/issues/8952 no way to print to stderr
    TestConfig(buildBal("echo"), skipTest = true) // note that skip test here only skips the stderr check
  }

  override val testUnicode = {
    TestConfig(buildBal("unicode"))
  }

  override val testEntryPointOtherThanMain = {
    TestConfig(buildBal("niam"), "niam", enforceEmptyOutputStream = false)
  }

  override val testLargeInput = {
    TestConfig(buildBal("return-response"))
  }

  it should "Initialize with the hello code and invoke" in {
    val (out, err) = withActionContainer() { c =>
      val sourceFile = buildBal("hello")
      sourceFile should not be "Build Error"

      val (initCode, _) = c.init(initPayload(sourceFile))
      initCode should be(200)

      val (runCode, runRes) = c.run(runPayload(JsObject()))
      runRes should be(Some(JsObject("response" -> JsString("hello-world"))))
    }
  }

  it should "Initialize with function returning the response and invoke it" in {
    val (out, err) = withActionContainer() { c =>
      val sourceFile = buildBal("return-response")
      sourceFile should not be "Build Error"

      val (initCode, _) = c.init(initPayload(sourceFile))
      initCode should be(200)

      val (runCode, runRes) = c.run(runPayload(JsObject("response" -> JsString("hello-world"))))
      runRes should be(Some(JsObject("response" -> JsString("hello-world"))))
    }
  }

  it should "should fail for Ballerina code with incompatible main" in {
    val (out, err) = withActionContainer() { c =>
      val sourceFile = buildBal("fail")
      sourceFile should not be "Build Error"

      val (initCode, _) = c.init(initPayload(sourceFile))
      initCode should be(200)

      val (runCode, _) = c.run(runPayload(JsObject("response" -> JsString("hello-world"))))
      runCode should not be (200)
    }
  }

  it should "fail to initialize with bad code" in {
    val (out, err) = withActionContainer() { c =>
      // This is valid zip file containing a single file, but not a valid
      // balx file.
      val brokenFile = ("UEsDBAoAAAAAAPxYbkhT4iFbCgAAAAoAAAANABwAbm90YWNsYXNzZmlsZVV" +
        "UCQADzNPmVszT5lZ1eAsAAQT1AQAABAAAAABzYXVjaXNzb24KUEsBAh4DCg" +
        "AAAAAA/FhuSFPiIVsKAAAACgAAAA0AGAAAAAAAAQAAAKSBAAAAAG5vdGFjb" +
        "GFzc2ZpbGVVVAUAA8zT5lZ1eAsAAQT1AQAABAAAAABQSwUGAAAAAAEAAQBT" +
        "AAAAUQAAAAAA")

      val (initCode, _) = c.init(initPayload("example.Broken", brokenFile))
      initCode should not be (200)
    }
  }

  it should "fail for a direct call to run response with 400" in {
    val (out, err) = withActionContainer() { c =>
      val (runCode, runRes) = c.run(runPayload(JsObject()))
      runCode should be(400)
    }
  }

  def buildBal(functionName: String): String = {
    // Set Ballerina home path to resolve dependency libs if necessary
    if (System.getProperty("ballerina.home") == null) {
      val ballerinaHome = Paths.get(System.getProperty("user.dir"), "..", "ballerina", "proxy", "build")
      System.setProperty("ballerina.home", ballerinaHome.toString)
    }

    val path = getClass.getResource("/".concat(functionName)).getPath
    val balxPath = Paths.get(path, functionName.concat(".balx"))
    val context = new CompilerContext
    val options = CompilerOptions.getInstance(context)

    options.put(PROJECT_DIR, path.toString)
    options.put(COMPILER_PHASE, CompilerPhase.CODE_GEN.toString)
    options.put(OFFLINE, "true")

    Files.deleteIfExists(balxPath)
    val compiler = Compiler.getInstance(context)
    try {
      val compiledPackage = compiler.build()
      compiledPackage should have size 1
      compiledPackage.forEach(pkg => compiler.write(pkg, balxPath.toString))
    } catch {
      case e: Exception => return "Build Error"
    }

    val diagnosticLog = BLangDiagnosticLog.getInstance(context)
    if (diagnosticLog.errorCount > 0) {
      return "Build Error"
    }

    val encoded = Base64.getEncoder.encode(Files.readAllBytes(balxPath))
    new String(encoded, "UTF-8")
  }

}
