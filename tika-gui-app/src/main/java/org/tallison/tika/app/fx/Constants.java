package org.tallison.tika.app.fx;

import org.apache.tika.pipes.fetcher.fs.FileSystemFetcher;

public class Constants {
    public static final String FS_EMITTER_CLASS =
            "org.apache.tika.pipes.emitter.fs.FileSystemEmitter";
    public static final String FS_FETCHER_CLASS = FileSystemFetcher.class.getName();
}
