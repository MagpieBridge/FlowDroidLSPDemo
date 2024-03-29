'use strict';

import * as path from 'path';

import {  workspace, ExtensionContext } from 'vscode';
import { LanguageClient, LanguageClientOptions, ServerOptions } from 'vscode-languageclient';

export function activate(context: ExtensionContext) {
    let script = 'java';
    let args = [ '-Xss1g', '-Xms1g', '-Xmx4g','-jar',context.asAbsolutePath(path.join('FlowDroidLSP-demo.jar')),"-c", context.asAbsolutePath('./config')];
    let serverOptions: ServerOptions = {
        run : { command: script, args: args },
        debug: { command: script, args: args} //, options: { env: createDebugEnv() }
    };
    
    let clientOptions: LanguageClientOptions = {
        documentSelector: [{ scheme: 'file', language: 'java' }],
        synchronize: {
            configurationSection: 'java',
            fileEvents: [ workspace.createFileSystemWatcher('**/*.java') ]
        }
    };
    
    // Create the language client and start the client.
    let lc : LanguageClient = new LanguageClient('FlowDroidLSP-demo','FlowDroid', serverOptions, clientOptions);
    lc.start();
}

