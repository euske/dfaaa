#!/bin/sh
# usage:
#   java2df.sh [opts] *.java
BASEDIR="${0%/*}/.."
LIBDIR="${BASEDIR}/lib"
JVMOPTS="-ea -XX:MaxJavaStackTraceDepth=1000000"
CLASSPATH="${BASEDIR}/target"
CLASSPATH="${CLASSPATH}:${LIBDIR}/bcel-6.2.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/junit-4.12.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/xmlunit-1.6.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.jdt.core-3.25.0.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.core.resources-3.14.0.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.core.runtime-3.20.100.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.core.filesystem-1.7.700.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.core.expressions-3.7.100.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.core.jobs-3.10.1100.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.core.contenttype-3.7.900.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.equinox.common-3.14.100.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.equinox.registry-3.10.100.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.equinox.preferences-3.8.200.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.equinox.app-1.5.100.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.text-3.11.0.jar"
CLASSPATH="${CLASSPATH}:${LIBDIR}/org.eclipse.osgi-3.16.200.jar"
exec java $JVMOPTS -cp "$CLASSPATH" net.tabesugi.fgyama.Java2DF "$@"
