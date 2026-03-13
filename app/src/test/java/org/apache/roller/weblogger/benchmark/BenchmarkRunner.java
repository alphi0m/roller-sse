/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.roller.weblogger.benchmark;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for running JMH benchmarks from IDE or Maven exec plugin.
 */
public final class BenchmarkRunner {

    private BenchmarkRunner() {
    }

    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 0) {
            org.openjdk.jmh.Main.main(args);
            return;
        }

        List<String> defaultArgs = new ArrayList<>();
        defaultArgs.add("org.apache.roller.weblogger.benchmark.*");
        defaultArgs.add("-bm");
        defaultArgs.add("avgt");
        defaultArgs.add("-tu");
        defaultArgs.add("us");
        defaultArgs.add("-wi");
        defaultArgs.add("2");
        defaultArgs.add("-i");
        defaultArgs.add("3");
        defaultArgs.add("-f");
        defaultArgs.add("0");

        org.openjdk.jmh.Main.main(defaultArgs.toArray(new String[0]));
    }
}
