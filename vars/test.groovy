#!/usr/bin/env groovy

def copyFilesMatching(searchDirs = ['.'], filenamePatterns = [], egrepInlcudeFilters = [], egrepExcludeFilters = []) {
    findCommand = buildFindCommand(searchDirs, filenamePatterns, egrepInlcudeFilters, egrepExcludeFilters)

    
}

def buildFindCommand(searchDirs = ['.'], filenamePatterns = [], egrepInlcudeFilters = ['.*'], egrepExcludeFilters = []) {
    def String command = 'find ' + shellQuoteAll(searchDirs).join(' ') + ' -type f '

    println command

    for ( patt in shellQuoteAll(filenamePatterns)) {
            command = command + ' -o -name ' + patt
    }

    println command

    if ( egrepInlcudeFilters.size() > 0 ) {
        command = command + ' | grep -E '
        for ( regex in shellQuoteAll(filenamePatterns) ) {
            command = command + ' -e ' + regex
        }
    }

    println command

    if ( egrepExcludeFilters.size() > 0 ) {
        command = command + ' | grep -E -v '
        for ( regex in shellQuoteAll(filenamePatterns) ) {
            command = command + ' -e ' + regex
        }
    }

    return command
}

def shellQuoteAll(strs = []) {
    def rv = [];
    for (s in strs) {
        rv.add("'" + s.replace("'", "'\\''") + "'")
    }

    return rv
}

copyFilesMatching(['~', '/var/log/jenkins', "/usr/local/var with spaces/an'stuff"], ['foo*','bar'], ['.*', 'patt'], ['fsd', 'sdsdfd'])
