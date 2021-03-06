/*
 * Copyright (C) 2011 Near Infinity Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nearinfinity.blur.mapreduce.example;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.DefaultCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import com.nearinfinity.blur.mapreduce.BlurTask;
import com.nearinfinity.blur.mapreduce.BlurTask.INDEXING_TYPE;
import com.nearinfinity.blur.thrift.generated.AnalyzerDefinition;
import com.nearinfinity.blur.thrift.generated.ColumnDefinition;
import com.nearinfinity.blur.thrift.generated.TableDescriptor;

public class BlurExampleIndexerRebuild {
  
  public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
    Configuration configuration = new Configuration();
    String[] otherArgs = new GenericOptionsParser(configuration, args).getRemainingArgs();
    if (otherArgs.length != 2) {
      System.err.println("Usage: blurindexer <in> <out>");
      System.exit(2);
    }

    AnalyzerDefinition ad = new AnalyzerDefinition();
    ad.defaultDefinition = new ColumnDefinition(StandardAnalyzer.class.getName(), true, null);

    TableDescriptor descriptor = new TableDescriptor();
    descriptor.analyzerDefinition = ad;
    descriptor.compressionBlockSize = 32768;
    descriptor.compressionClass = DefaultCodec.class.getName();
    descriptor.isEnabled = true;
    descriptor.name = "test-table";
    descriptor.shardCount = 1;
    descriptor.cluster = "default";
    descriptor.tableUri = "./blur-testing";
    
    BlurTask blurTask = new BlurTask();
    blurTask.setTableDescriptor(descriptor);
    blurTask.setIndexingType(INDEXING_TYPE.REBUILD);
    blurTask.setOptimize(false);
    Job job = blurTask.configureJob(configuration);
    job.setJarByClass(BlurExampleIndexerRebuild.class);
    job.setMapperClass(BlurExampleMapper.class);
    job.setInputFormatClass(TextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);
    
    FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
    FileOutputFormat.setOutputPath(job, new Path(otherArgs[1], "job-" + System.currentTimeMillis()));
    long s = System.currentTimeMillis();
    boolean waitForCompletion = job.waitForCompletion(true);
    long e = System.currentTimeMillis();
    System.out.println("Completed in [" + (e-s) + " ms]");
    System.exit(waitForCompletion ? 0 : 1);
  }
}
