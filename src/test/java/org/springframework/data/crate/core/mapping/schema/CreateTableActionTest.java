/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.crate.core.mapping.schema;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.data.crate.core.CrateSQLAction;
import org.springframework.data.crate.core.mapping.annotations.Table;

/**
 * @author Hasnain Javed
 * @since 1.0.0
 */
public class CreateTableActionTest {

	@Test
	public void shouldCreateStatementWithPrimaryKeyColumn() {
		
		Column longCol = createColumn("longField", Long.class, null, true);
		TableDefinition tableDefinition = createTableDefinition("entity", longCol);
		
		CrateSQLAction action = new CreateTableAction(tableDefinition);
		
		assertThat(action.getSQLStatement(), is("create table entity (\"longField\" long primary key)"));
	}
	
	@Test
	public void shouldCreateStatementWithPrimitiveColumn() {
		
		Column stringCol = createColumn("stringField", String.class, null, true);
		Column intCol = createColumn("integerField", Integer.class, null, false);
		TableDefinition tableDefinition = createTableDefinition("entity", stringCol, intCol);
		
		CrateSQLAction action = new CreateTableAction(tableDefinition);
		
		assertThat(action.getSQLStatement(), is("create table entity (\"stringField\" string primary key, \"integerField\" integer)"));
	}
	
	@Test
	public void shouldCreateStatementWithPrimitiveCollection() {
		
		Column arrayCol = createColumn("integers", Integer[].class, Integer.class, null);
		TableDefinition tableDefinition = createTableDefinition("entity", arrayCol);

		CrateSQLAction action = new CreateTableAction(tableDefinition);
		
		assertThat(action.getSQLStatement(), is("create table entity (\"integers\" array(integer))"));
	}
	
	@Test
	public void shouldCreateStatementWithMap() {
		
		Column mapCol = createColumn("map", Map.class, null, null);
		TableDefinition tableDefinition = createTableDefinition("entity", mapCol);
		
		CrateSQLAction action = new CreateTableAction(tableDefinition);
		
		assertThat(action.getSQLStatement(), is("create table entity (\"map\" object)"));
	}
	
	@Test
	public void shouldCreateStatementWithNestedEntity() {
		
		Column stringCol = createColumn("stringField", String.class, null, null);
		Column intCol = createColumn("integerField", Integer.class, null, null);
		Column objectCol = createColumn("nestedEntity", EntityWithPrimitives.class, null, null);
		objectCol.setSubColumns(asList(stringCol, intCol));
		
		Column rootStringCol = createColumn("stringField", String.class, null, null);
		
		TableDefinition tableDefinition = createTableDefinition("entity", rootStringCol, objectCol);
		
		CrateSQLAction action = new CreateTableAction(tableDefinition);
		
		StringBuilder sql = new StringBuilder("create table entity (\"stringField\" string, \"nestedEntity\" object as (");
		sql.append("\"stringField\" string, \"integerField\" integer))");
		
		assertThat(action.getSQLStatement(), is(sql.toString()));
	}
	
	@Test
	public void shouldCreateStatementWithEntityArray() {
		
		Column stringCol = createColumn("stringField", String.class, null, null);
		Column intCol = createColumn("integerField", Integer.class, null, null);
		Column objectCol = createColumn("nestedEntities", EntityWithPrimitives[].class, EntityWithPrimitives.class, null);
		objectCol.setSubColumns(asList(stringCol, intCol));
		
		Column rootStringCol = createColumn("stringField", String.class, null, null);
		
		TableDefinition tableDefinition = createTableDefinition("entity", rootStringCol, objectCol);
		
		CrateSQLAction action = new CreateTableAction(tableDefinition);
		
		StringBuilder sql = new StringBuilder("create table entity (\"stringField\" string, \"nestedEntities\" array(");
		sql.append("object as (\"stringField\" string, \"integerField\" integer)))");
		
		assertThat(action.getSQLStatement(), is(sql.toString()));
	}
	
	@Test
	public void shouldCreateStatementWithNestedEntityCollection() {
		
		Column stringColLevel2 = createColumn("stringField", String.class, null, null);
		Column intColLevel2 = createColumn("integerField", Integer.class, null, null);
		
		Column stringColLevel1 = createColumn("stringField", String.class, null, null);
		Column objectColLevel1 = createColumn("nested", EntityWithPrimitives.class, null, null);
		objectColLevel1.setSubColumns(asList(stringColLevel2, intColLevel2));
		
		Column rootStringCol = createColumn("stringField", String.class, null, null);
		Column rootArrayCol = createColumn("nestedEntities", Set.class, EntityWithNestedEntity.class, null);
		rootArrayCol.setSubColumns(asList(stringColLevel1, objectColLevel1));
		
		TableDefinition tableDefinition = createTableDefinition("entity", rootStringCol, rootArrayCol);
		
		CrateSQLAction action = new CreateTableAction(tableDefinition);
		
		StringBuilder sql = new StringBuilder("create table entity (\"stringField\" string, \"nestedEntities\" array(");
		sql.append("object as (\"stringField\" string, \"nested\" object as (");
		sql.append("\"stringField\" string, \"integerField\" integer))))");
		
		assertThat(action.getSQLStatement(), is(sql.toString()));
	}
	
	private TableDefinition createTableDefinition(String name, Column... columns) {
		return new TableDefinition(name, asList(columns));
	}
	
	private Column createColumn(String name, Class<?> type, Class<?> elementType, Boolean primaryKey) {
		Column column = null;
		if(elementType != null) {
			column = new Column(name, type, elementType);
		}else {
			column = new Column(name, type);
		}
		if(primaryKey != null) {
			column.setPrimaryKey(primaryKey);
		}
		return column;
	}
	
	@Table(name="entity")
	static class EntityWithPrimitives {
		String stringField;
		int integerField;
	}
	
	@Table(name="entity")
	static class EntityWithNestedEntity {
		String stringField;
		EntityWithPrimitives nested;
	}
	
	@Table(name="entity")
	static class EntityWithEntityCollection {
		String stringField;
		Set<EntityWithPrimitives> nestedEntities;
	}
	
	@Table(name="entity")
	static class EntityWithNestedEntityCollection {
		String stringField;
		Set<EntityWithNestedEntity> nestedEntities;
	}
}