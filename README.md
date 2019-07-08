## Build FlowDroidLSPDemo with Maven

1. check out adapted FlowDroid with
```git checkout -b lsp https://github.com/linghuiluo/FlowDroid.git```
2. install FlowDroid into your local maven repository with `mvn install -DskipTests`
3. A jar file called `flowdroid-lsp-demo-0.0.1-SNAPSHOT.jar` will be created in the `target` directory. Use it to configure the a language server in an editor following the step 11 from this [tutorial](https://github.com/MagpieBridge/MagpieBridge/wiki/Create-your-first-project-with-MagpieBridge) 

## Run FlowDroidLSP in IDEs
1. Run FlowDroidLSPDemo in this [demo Java Project]() in Eclipse: 
  - use the following arguments for the lanuch configuration 
   ```-jar PATH_TO_LOCAL_REPO\flowdroid-lsp-demo\target\flowdroid-lsp-demo-0.0.1-SNAPSHOT.jar -c  PATH_TO_LOCAL_REPO\flowdroid-lsp-demo\config```
  - Import [DemoFlowDroid]() as Maven project in Eclipse.
  - Open a Java file in this project, this will trigger the server to run FlowDroid.

2. Run FlowDroidLSPDemo in this [demo Java Project]() in Visual Studio Code:
  - navigate to `PATH_TO_LOCAL_REPO\flowdroid-lsp-demo\vscode`
  - run `vsce package`
  - install the FlowDroidLSP extension with `code --install-extension FlowDroidLSP-0.0.1.vsix`
  [![VSCodeDemo](https://img.youtube.com/vi/S89_V9DGtrk/0.jpg)](http://www.youtube.com/watch?v=S89_V9DGtrk)

3. Run FlowDroidLSPDemo in this [demo Android Project]() in Android Stuido:
  - install LSP Plugin in Android Studio
  - use the following arguments for defining a language server
    java -jar "PATH_TO_LOCAL_REPO\flowdroid-lsp-demo\target\flowdroid-lsp-demo-0.0.1-SNAPSHOT.jar" -c "PATH_TO_LOCAL_REPO\flowdroid-lsp-demo\config" -a -p "PATH_TO_ANDROID_PLATFORMS"
   - Open [DemoFlowDroidAndroid]() in Android Studio.
   - Open a Java file in this project, this will trigger the server to run FlowDroid.
  [![AndroidStudioDemo](https://img.youtube.com/vi/1kDWslIjPus/0.jpg)](http://www.youtube.com/watch?v=1kDWslIjPus)