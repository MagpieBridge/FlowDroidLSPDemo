'use strict';

import * as path from 'path';

import {  workspace, ExtensionContext } from 'vscode';
import { LanguageClient, LanguageClientOptions, ServerOptions } from 'vscode-languageclient';

export function activate(context: ExtensionContext) {
    let script = 'java';
    let args = ['-jar',context.asAbsolutePath(path.join('flowdroid-lsp-demo.jar')),"-c", context.asAbsolutePath('.')];
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
    let lc : LanguageClient = new LanguageClient('flowdroid-lsp-demo','FlowDroid LSP Demo Server', serverOptions, clientOptions);
    lc.start();
}

