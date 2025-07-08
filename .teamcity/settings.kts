import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.NodeJSBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.nodeJS
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.buildSteps.python
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.matrix
import jetbrains.buildServer.configs.kotlin.projectFeatures.activeStorage
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.schedule
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2025.03"

project {

    vcsRoot(testdemo)

    buildType(Build)
    buildType(Deploy)
    buildType(Firststep)
    buildType(BuildCon)

    features {
        activeStorage {
            id = "PROJECT_EXT_59"
            activeStorageID = "DefaultStorage"
        }
    }
}

object Build : BuildType({
    name = "Build"

    artifactRules = "**/* => teamcity.zip"

    params {
        param("build.vcs.number", "%system.build.number%")
        param("teamcity.configuration.tags", "tag2")
        param("USE_EXPERIMENTAL_AGENT", "true")
        param("build.triggered.by.schedule", "false")
    }

    vcs {
        root(testdemo)
    }

    steps {
        script {
            id = "simpleRunner"
            enabled = false
            scriptContent = """
                SHORT_HASH=${'$'}(echo '1234567890' | cut -c1-7)
                echo "##teamcity[buildNumber '1.0.%build.counter%.${'$'}SHORT_HASH']"
            """.trimIndent()
        }
        powerShell {
            id = "jetbrains_powershell"
            enabled = false
            scriptMode = script {
                content = """
                    # Define variables
                    ${'$'}teamCityServer = "%teamcity.serverUrl%"
                    ${'$'}buildTypeId = "AllBuild_Deploy" # 
                    ${'$'}username = "%system.teamcity.auth.userId%"
                    ${'$'}password = "%system.teamcity.auth.password%"
                    
                    # Optional: Parameters for the build
                    if ("true" -eq "%build.triggered.by.schedule%") {
                    # Convert parameters to JSON if present
                    ${'$'}jsonBody = @{
                            "buildType" = @{
                                "id" = ${'$'}buildTypeId
                            }
                        } | ConvertTo-Json -Depth 10
                    ${'$'}base64AuthInfo = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes(("{0}:{1}" -f ${'$'}username,${'$'}password)))    
                        # Encode credentials in base64
                    ${'$'}authHeader = "Basic " + ${'$'}base64AuthInfo
                    
                    # Define the REST API endpoint
                    ${'$'}apiUrl = "${'$'}teamCityServer/app/rest/buildQueue"
                    
                    # Invoke the REST API to trigger the build
                    ${'$'}response = Invoke-RestMethod -Uri ${'$'}apiUrl -Method Post -Headers @{
                        "Authorization" = ${'$'}authHeader
                        "Content-Type"  = "application/json"
                    } -Body ${'$'}jsonBody
                    
                    # Output the response
                    Write-Host "Build Deploy triggered. Response:" -ForegroundColor Green
                    ${'$'}response
                    }
                     else {
                    Write-Host "Build Deploy is not triggered. Response:" -ForegroundColor Green
                      
                    }
                """.trimIndent()
            }
        }
        script {
            id = "simpleRunner_1"
            enabled = false
            scriptContent = """
                ssh-add -l
                GIT_SSH_COMMAND="ssh -vvv" git clone git@github.com:TomSunGit/DesktopDemo.git
            """.trimIndent()
        }
        script {
            id = "simpleRunner_2"
            enabled = false
            scriptContent = "echo test"
        }
        nodeJS {
            id = "nodejs_runner"
            enabled = false
            shellScript = "npm --version"
            dockerImagePlatform = NodeJSBuildStep.ImagePlatform.Any
        }
        script {
            id = "simpleRunner_3"
            scriptContent = "echo test"
        }
        script {
            id = "simpleRunner_4"
            enabled = false
            scriptContent = """echo "##teamcity[skipQueuedBuilds tags='AllBuild_Deploy,tag-test' comment='Your comment']""""
        }
        script {
            id = "simpleRunner_5"
            enabled = false
            scriptContent = "sleep 300"
        }
        python {
            id = "python_runner"
            enabled = false
            command = script {
                content = "--version"
            }
        }
    }

    triggers {
        schedule {
            schedulingPolicy = cron {
                minutes = "28"
            }
            branchFilter = ""
            triggerBuild = always()
            withPendingChangesOnly = false
            param("dayOfWeek", "Tuesday")
            param("hour", "17")

            buildParams {
                param("build.triggered.by.schedule", "true")
            }
        }
    }

    features {
        perfmon {
        }
        sshAgent {
            teamcitySshKey = "test-build-agent-feature"
        }
        matrix {
            param("OS.platform", listOf(
                value("Windows")
            ))
        }
    }

    dependencies {
        snapshot(Firststep) {
        }
    }

    requirements {
        equals("agent.type", "stable", "RQ_8")
        equals("agent.type", "experimental", "RQ_9")
        equals("USE_EXPERIMENTAL_AGENT", "true", "RQ_11")
    }
    
    disableSettings("RQ_11", "RQ_8", "RQ_9")
})

object BuildCon : BuildType({
    name = "Build-Con"

    type = BuildTypeSettings.Type.COMPOSITE

    vcs {
        root(DslContext.settingsRoot)

        showDependenciesChanges = true
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
    }
})

object Deploy : BuildType({
    name = "Deploy"

    artifactRules = "**/* => deploy.zip"

    params {
        param("teamcity.configuration.tags", "tag3")
    }

    steps {
        script {
            id = "simpleRunner"
            scriptContent = "echo deploy"
        }
        script {
            id = "simpleRunner_1"
            enabled = false
            scriptContent = "echo ##teamcity[skipQueuedBuilds tags='AllBuild_Build,tag2' comment='Your comment']"
        }
        script {
            id = "simpleRunner_2"
            scriptContent = "sleep 15"
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${Build.id}"
            successfulOnly = true
        }
    }

    dependencies {
        snapshot(Build) {
        }
    }
})

object Firststep : BuildType({
    name = "Firststep"

    steps {
        script {
            id = "simpleRunner_1"
            scriptContent = """echo "##teamcity[skipQueuedBuilds tags='tag2,tag3,tag4' comment='Your comment']""""
        }
        script {
            name = "sleep 5"
            id = "simpleRunner"
            scriptContent = "sleep 5"
        }
    }
})

object testdemo : GitVcsRoot({
    name = "testdemo"
    url = "https://github.com/TomSunGit/tomdemo"
    branch = "refs/heads/main"
    authMethod = password {
        userName = "TomSunGit"
        password = "credentialsJSON:32e71d32-2533-49dc-993f-90c5f1541cec"
    }
})
