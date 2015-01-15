/*
 * Copyright (c) 2007-2015 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cascading;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import cascading.flow.Flow;
import cascading.flow.FlowProcess;
import cascading.operation.Aggregator;
import cascading.operation.Buffer;
import cascading.operation.ConcreteCall;
import cascading.operation.Filter;
import cascading.operation.Function;
import cascading.tap.Tap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleEntryIterator;
import cascading.tuple.TupleListCollector;
import junit.framework.TestCase;

/**
 * Class CascadingTestCase is the base class for all Cascading tests.
 * <p/>
 * It included a few helpful utility methods for testing Cascading applications.
 */
public class CascadingTestCase extends TestCase implements Serializable
  {
  static class TestFlowProcess extends FlowProcess.NullFlowProcess
    {
    private final Map<Object, Object> properties;

    public TestFlowProcess( Map<Object, Object> properties )
      {
      this.properties = properties;
      }

    @Override
    public Object getProperty( String key )
      {
      return properties.get( key );
      }
    }

  public CascadingTestCase()
    {
    }

  public CascadingTestCase( String name )
    {
    super( name );
    }

  public static void validateLength( Flow flow, int length ) throws IOException
    {
    validateLength( flow, length, -1 );
    }

  public static void validateLength( Flow flow, int length, String name ) throws IOException
    {
    validateLength( flow, length, -1, null, name );
    }

  public static void validateLength( Flow flow, int length, int size ) throws IOException
    {
    validateLength( flow, length, size, null, null );
    }

  public static void validateLength( Flow flow, int length, int size, Pattern regex ) throws IOException
    {
    validateLength( flow, length, size, regex, null );
    }

  public static void validateLength( Flow flow, int length, Pattern regex, String name ) throws IOException
    {
    validateLength( flow, length, -1, regex, name );
    }

  public static void validateLength( Flow flow, int length, int size, Pattern regex, String name ) throws IOException
    {
    TupleEntryIterator iterator = name == null ? flow.openSink() : flow.openSink( name );
    validateLength( iterator, length, size, regex );
    }

  public static void validateLength( TupleEntryIterator iterator, int length )
    {
    validateLength( iterator, length, -1, null );
    }

  public static void validateLength( TupleEntryIterator iterator, int length, int size )
    {
    validateLength( iterator, length, size, null );
    }

  public static void validateLength( TupleEntryIterator iterator, int length, Pattern regex )
    {
    validateLength( iterator, length, -1, regex );
    }

  public static void validateLength( TupleEntryIterator iterator, int length, int size, Pattern regex )
    {
    int count = 0;

    while( iterator.hasNext() )
      {
      TupleEntry tupleEntry = iterator.next();

      if( size != -1 )
        assertEquals( "wrong number of elements", size, tupleEntry.size() );

      if( regex != null )
        assertTrue( "regex: " + regex + " does not match: " + tupleEntry.getTuple().toString(), regex.matcher( tupleEntry.getTuple().toString() ).matches() );

      count++;
      }

    try
      {
      iterator.close();
      }
    catch( IOException exception )
      {
      throw new RuntimeException( exception );
      }

    assertEquals( "wrong number of lines", length, count );
    }

  public static TupleListCollector invokeFunction( Function function, Tuple arguments, Fields resultFields )
    {
    return invokeFunction( function, new TupleEntry( arguments ), resultFields );
    }

  public static TupleListCollector invokeFunction( Function function, Tuple arguments, Fields resultFields, Map<Object, Object> properties )
    {
    return invokeFunction( function, new TupleEntry( arguments ), resultFields, properties );
    }

  public static TupleListCollector invokeFunction( Function function, TupleEntry arguments, Fields resultFields )
    {
    return invokeFunction( function, arguments, resultFields, new HashMap<Object, Object>() );
    }

  public static TupleListCollector invokeFunction( Function function, TupleEntry arguments, Fields resultFields, Map<Object, Object> properties )
    {
    FlowProcess flowProcess = new TestFlowProcess( properties );
    ConcreteCall operationCall = new ConcreteCall( arguments.getFields() );
    TupleListCollector collector = new TupleListCollector( resultFields, true );

    operationCall.setArguments( arguments );
    operationCall.setOutputCollector( collector );

    function.prepare( flowProcess, operationCall );
    function.operate( flowProcess, operationCall );
    function.cleanup( flowProcess, operationCall );

    return collector;
    }

  public static TupleListCollector invokeFunction( Function function, Tuple[] argumentsArray, Fields resultFields )
    {
    TupleEntry[] entries = makeArgumentsArray( argumentsArray );

    return invokeFunction( function, entries, resultFields );
    }

  public static TupleListCollector invokeFunction( Function function, Tuple[] argumentsArray, Fields resultFields, Map<Object, Object> properties )
    {
    TupleEntry[] entries = makeArgumentsArray( argumentsArray );

    return invokeFunction( function, entries, resultFields, properties );
    }

  public static TupleListCollector invokeFunction( Function function, TupleEntry[] argumentsArray, Fields resultFields )
    {
    return invokeFunction( function, argumentsArray, resultFields, new HashMap<Object, Object>() );
    }

  public static TupleListCollector invokeFunction( Function function, TupleEntry[] argumentsArray, Fields resultFields, Map<Object, Object> properties )
    {
    FlowProcess flowProcess = new TestFlowProcess( properties );
    ConcreteCall operationCall = new ConcreteCall( argumentsArray[ 0 ].getFields() );
    TupleListCollector collector = new TupleListCollector( resultFields, true );

    function.prepare( flowProcess, operationCall );
    operationCall.setOutputCollector( collector );

    for( TupleEntry arguments : argumentsArray )
      {
      operationCall.setArguments( arguments );
      function.operate( flowProcess, operationCall );
      }

    function.flush( flowProcess, operationCall );
    function.cleanup( flowProcess, operationCall );

    return collector;
    }

  public static boolean invokeFilter( Filter filter, Tuple arguments )
    {
    return invokeFilter( filter, new TupleEntry( arguments ) );
    }

  public static boolean invokeFilter( Filter filter, Tuple arguments, Map<Object, Object> properties )
    {
    return invokeFilter( filter, new TupleEntry( arguments ), properties );
    }

  public static boolean invokeFilter( Filter filter, TupleEntry arguments )
    {
    return invokeFilter( filter, arguments, new HashMap<Object, Object>() );
    }

  public static boolean invokeFilter( Filter filter, TupleEntry arguments, Map<Object, Object> properties )
    {
    FlowProcess flowProcess = new TestFlowProcess( properties );
    ConcreteCall operationCall = new ConcreteCall( arguments.getFields() );

    operationCall.setArguments( arguments );

    filter.prepare( flowProcess, operationCall );

    boolean isRemove = filter.isRemove( flowProcess, operationCall );

    filter.cleanup( flowProcess, operationCall );

    return isRemove;
    }

  public static boolean[] invokeFilter( Filter filter, Tuple[] argumentsArray )
    {
    TupleEntry[] entries = makeArgumentsArray( argumentsArray );

    return invokeFilter( filter, entries, Collections.emptyMap() );
    }

  public static boolean[] invokeFilter( Filter filter, Tuple[] argumentsArray, Map<Object, Object> properties )
    {
    TupleEntry[] entries = makeArgumentsArray( argumentsArray );

    return invokeFilter( filter, entries, properties );
    }

  public static boolean[] invokeFilter( Filter filter, TupleEntry[] argumentsArray )
    {
    return invokeFilter( filter, argumentsArray, Collections.emptyMap() );
    }

  public static boolean[] invokeFilter( Filter filter, TupleEntry[] argumentsArray, Map<Object, Object> properties )
    {
    ConcreteCall operationCall = new ConcreteCall( argumentsArray[ 0 ].getFields() );

    FlowProcess flowProcess = new TestFlowProcess( properties );

    filter.prepare( flowProcess, operationCall );

    boolean[] results = new boolean[ argumentsArray.length ];

    for( int i = 0; i < argumentsArray.length; i++ )
      {
      operationCall.setArguments( argumentsArray[ i ] );

      results[ i ] = filter.isRemove( flowProcess, operationCall );
      }

    filter.flush( flowProcess, operationCall );
    filter.cleanup( flowProcess, operationCall );

    return results;
    }

  public static TupleListCollector invokeAggregator( Aggregator aggregator, Tuple[] argumentsArray, Fields resultFields )
    {
    TupleEntry[] entries = makeArgumentsArray( argumentsArray );

    return invokeAggregator( aggregator, entries, resultFields );
    }

  public static TupleListCollector invokeAggregator( Aggregator aggregator, Tuple[] argumentsArray, Fields resultFields, Map<Object, Object> properties )
    {
    TupleEntry[] entries = makeArgumentsArray( argumentsArray );

    return invokeAggregator( aggregator, entries, resultFields, properties );
    }

  public static TupleListCollector invokeAggregator( Aggregator aggregator, TupleEntry[] argumentsArray, Fields resultFields )
    {
    return invokeAggregator( aggregator, null, argumentsArray, resultFields );
    }

  public static TupleListCollector invokeAggregator( Aggregator aggregator, TupleEntry[] argumentsArray, Fields resultFields, Map<Object, Object> properties )
    {
    return invokeAggregator( aggregator, null, argumentsArray, resultFields, properties );
    }

  public static TupleListCollector invokeAggregator( Aggregator aggregator, TupleEntry group, TupleEntry[] argumentsArray, Fields resultFields )
    {
    return invokeAggregator( aggregator, group, argumentsArray, resultFields, Collections.emptyMap() );
    }

  public static TupleListCollector invokeAggregator( Aggregator aggregator, TupleEntry group, TupleEntry[] argumentsArray, Fields resultFields, Map<Object, Object> properties )
    {
    FlowProcess flowProcess = new TestFlowProcess( properties );
    ConcreteCall operationCall = new ConcreteCall( argumentsArray[ 0 ].getFields() );

    operationCall.setGroup( group );

    aggregator.prepare( flowProcess, operationCall );

    aggregator.start( flowProcess, operationCall );

    for( TupleEntry arguments : argumentsArray )
      {
      operationCall.setArguments( arguments );
      aggregator.aggregate( flowProcess, operationCall );
      }

    TupleListCollector collector = new TupleListCollector( resultFields, true );
    operationCall.setOutputCollector( collector );

    aggregator.complete( flowProcess, operationCall );

    aggregator.cleanup( null, operationCall );

    return collector;
    }

  public static TupleListCollector invokeBuffer( Buffer buffer, Tuple[] argumentsArray, Fields resultFields )
    {
    TupleEntry[] entries = makeArgumentsArray( argumentsArray );

    return invokeBuffer( buffer, entries, resultFields );
    }

  public static TupleListCollector invokeBuffer( Buffer buffer, Tuple[] argumentsArray, Fields resultFields, Map<Object, Object> properties )
    {
    TupleEntry[] entries = makeArgumentsArray( argumentsArray );

    return invokeBuffer( buffer, entries, resultFields, properties );
    }

  public static TupleListCollector invokeBuffer( Buffer buffer, TupleEntry[] argumentsArray, Fields resultFields )
    {
    return invokeBuffer( buffer, null, argumentsArray, resultFields );
    }

  public static TupleListCollector invokeBuffer( Buffer buffer, TupleEntry[] argumentsArray, Fields resultFields, Map<Object, Object> properties )
    {
    return invokeBuffer( buffer, null, argumentsArray, resultFields, properties );
    }

  public static TupleListCollector invokeBuffer( Buffer buffer, TupleEntry group, TupleEntry[] argumentsArray, Fields resultFields )
    {
    return invokeBuffer( buffer, group, argumentsArray, resultFields, Collections.emptyMap() );
    }

  public static TupleListCollector invokeBuffer( Buffer buffer, TupleEntry group, TupleEntry[] argumentsArray, Fields resultFields, Map<Object, Object> properties )
    {
    FlowProcess flowProcess = new TestFlowProcess( properties );
    ConcreteCall operationCall = new ConcreteCall( argumentsArray[ 0 ].getFields() );

    operationCall.setGroup( group );

    buffer.prepare( flowProcess, operationCall );
    TupleListCollector collector = new TupleListCollector( resultFields, true );
    operationCall.setOutputCollector( collector );

    operationCall.setArgumentsIterator( Arrays.asList( argumentsArray ).iterator() );

    buffer.operate( flowProcess, operationCall );

    buffer.cleanup( null, operationCall );

    return collector;
    }

  private static TupleEntry[] makeArgumentsArray( Tuple[] argumentsArray )
    {
    TupleEntry[] entries = new TupleEntry[ argumentsArray.length ];

    for( int i = 0; i < argumentsArray.length; i++ )
      entries[ i ] = new TupleEntry( argumentsArray[ i ] );

    return entries;
    }

  public static List<Tuple> getSourceAsList( Flow flow ) throws IOException
    {
    return asCollection( flow, (Tap) flow.getSourcesCollection().iterator().next(), Fields.ALL, new ArrayList<Tuple>() );
    }

  public static List<Tuple> getSinkAsList( Flow flow ) throws IOException
    {
    return asCollection( flow, flow.getSink(), Fields.ALL, new ArrayList<Tuple>() );
    }

  public static List<Tuple> asList( Flow flow, Tap tap ) throws IOException
    {
    return asCollection( flow, tap, Fields.ALL, new ArrayList<Tuple>() );
    }

  public static List<Tuple> asList( Flow flow, Tap tap, Fields selector ) throws IOException
    {
    return asCollection( flow, tap, selector, new ArrayList<Tuple>() );
    }

  public static Set<Tuple> asSet( Flow flow, Tap tap ) throws IOException
    {
    return asCollection( flow, tap, Fields.ALL, new HashSet<Tuple>() );
    }

  public static Set<Tuple> asSet( Flow flow, Tap tap, Fields selector ) throws IOException
    {
    return asCollection( flow, tap, selector, new HashSet<Tuple>() );
    }

  public static <C extends Collection<Tuple>> C asCollection( Flow flow, Tap tap, C collection ) throws IOException
    {
    return asCollection( flow, tap, Fields.ALL, collection );
    }

  public static <C extends Collection<Tuple>> C asCollection( Flow flow, Tap tap, Fields selector, C collection ) throws IOException
    {
    TupleEntryIterator iterator = flow.openTapForRead( tap );

    try
      {
      return asCollection( iterator, selector, collection );
      }
    finally
      {
      iterator.close();
      }
    }

  public static <C extends Collection<Tuple>> C asCollection( TupleEntryIterator iterator, C result )
    {
    while( iterator.hasNext() )
      result.add( iterator.next().getTupleCopy() );

    return result;
    }

  public static <C extends Collection<Tuple>> C asCollection( TupleEntryIterator iterator, Fields selector, C result )
    {
    while( iterator.hasNext() )
      result.add( iterator.next().selectTupleCopy( selector ) );

    return result;
    }
  }
