/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.commandline.dbms;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

import org.neo4j.commandline.admin.IncorrectUsage;
import org.neo4j.commandline.admin.NullOutsideWorld;
import org.neo4j.commandline.admin.OutsideWorld;
import org.neo4j.commandline.admin.RealOutsideWorld;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.Args;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.test.rule.TestDirectory;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImportCommandTest
{
    @Rule
    public final TestDirectory testDir = TestDirectory.testDirectory();

    @Test
    public void defaultsToCsvWhenModeNotSpecified() throws Exception
    {
        File homeDir = testDir.directory( "home" );
        ImporterFactory mockImporterFactory = mock( ImporterFactory.class );
        when( mockImporterFactory
                .getImporterForMode( eq( "csv" ), any( Args.class ), any( Config.class ), any( OutsideWorld.class ) ) )
                .thenReturn( mock( Importer.class ) );

        ImportCommand importCommand =
                new ImportCommand( homeDir.toPath(), testDir.directory( "conf" ).toPath(), new RealOutsideWorld(),
                        mockImporterFactory );

        String[] arguments = {"--database=foo", "--from=bar"};

        importCommand.execute( arguments );

        verify( mockImporterFactory )
                .getImporterForMode( eq( "csv" ), any( Args.class ), any( Config.class ), any( OutsideWorld.class ) );
    }

    @Test
    public void requiresDatabaseArgument() throws Exception
    {
        ImportCommand importCommand =
                new ImportCommand( testDir.directory( "home" ).toPath(), testDir.directory( "conf" ).toPath(),
                        new NullOutsideWorld() );

        String[] arguments = {"--mode=database", "--from=bar"};
        try
        {
            importCommand.execute( arguments );
            fail( "Should have thrown an exception." );
        }
        catch ( IncorrectUsage e )
        {
            assertThat( e.getMessage(), containsString( "database" ) );
        }
    }

    @Test
    public void failIfInvalidModeSpecified() throws Exception
    {
        ImportCommand importCommand =
                new ImportCommand( testDir.directory( "home" ).toPath(), testDir.directory( "conf" ).toPath(),
                        new NullOutsideWorld() );

        String[] arguments = {"--mode=foo", "--database=bar", "--from=baz"};
        try
        {
            importCommand.execute( arguments );
            fail( "Should have thrown an exception." );
        }
        catch ( IncorrectUsage e )
        {
            assertThat( e.getMessage(), containsString( "foo" ) );
        }
    }

    @Test
    public void failIfDestinationDatabaseAlreadyExists() throws Exception
    {
        Path homeDir = testDir.directory( "home" ).toPath();
        ImportCommand importCommand =
                new ImportCommand( homeDir, testDir.directory( "conf" ).toPath(), new NullOutsideWorld() );

        putStoreInDirectory( homeDir.resolve( "data" ).resolve( "databases" ).resolve( "existing.db" ).toFile() );
        String[] arguments = {"--mode=csv", "--database=existing.db"};
        try
        {
            importCommand.execute( arguments );
            fail( "Should have thrown an exception." );
        }
        catch ( Exception e )
        {
            assertThat( e.getMessage(), containsString( "already contains a database" ) );
        }
    }

    private File putStoreInDirectory( File storeDir )
    {
        GraphDatabaseService db = null;
        try
        {
            db = new TestGraphDatabaseFactory().newEmbeddedDatabase( storeDir );
            try ( Transaction transaction = db.beginTx() )
            {
                db.createNode();
                transaction.success();
            }
        }
        finally
        {
            if ( db != null )
            {
                db.shutdown();
            }
        }

        return storeDir;
    }
}
