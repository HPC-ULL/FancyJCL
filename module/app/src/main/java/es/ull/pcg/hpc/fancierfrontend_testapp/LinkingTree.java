package es.ull.pcg.hpc.fancierfrontend_testapp;

import timber.log.Timber;

class LinkingTree extends Timber.DebugTree {
    @Override
    protected String createStackElementTag(StackTraceElement element) {
        return String.format("FSC.(%s:%s)%s", element.getFileName(), element.getLineNumber(), element.getMethodName());
    }
}
