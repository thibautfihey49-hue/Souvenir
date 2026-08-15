@rem
@rem Gradle startup script for Windows
@rem
@setlocal
@set APP_HOME=%~dp0
@set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
@java -Xmx64m -Xms64m -classpath "%CLASSPATH%" -Dorg.gradle.appname=%~nx0 org.gradle.wrapper.GradleWrapperMain %*
@endlocal
