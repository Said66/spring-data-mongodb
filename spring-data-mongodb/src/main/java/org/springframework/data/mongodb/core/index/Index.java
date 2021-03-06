/*
 * Copyright 2010-2017 the original author or authors.
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
package org.springframework.data.mongodb.core.index;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Order;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@SuppressWarnings("deprecation")
public class Index implements IndexDefinition {

	public enum Duplicates {
		RETAIN, //

		/**
		 * Dropping Duplicates was removed in MongoDB Server 2.8.0-rc0.
		 * <p>
		 * See https://jira.mongodb.org/browse/SERVER-14710
		 * 
		 * @deprecated since 1.7.
		 */
		@Deprecated //
		DROP
	}

	private final Map<String, Direction> fieldSpec = new LinkedHashMap<String, Direction>();

	private String name;

	private boolean unique = false;

	private boolean dropDuplicates = false;

	private boolean sparse = false;

	private boolean background = false;

	private long expire = -1;

	private IndexFilter filter;

	public Index() {}

	public Index(String key, Direction direction) {
		fieldSpec.put(key, direction);
	}

	/**
	 * Creates a new {@link Indexed} on the given key and {@link Order}.
	 * 
	 * @deprecated use {@link #Index(String, Direction)} instead.
	 * @param key must not be {@literal null} or empty.
	 * @param order must not be {@literal null}.
	 */
	@Deprecated
	public Index(String key, Order order) {
		this(key, order.toDirection());
	}

	/**
	 * Adds the given field to the index.
	 * 
	 * @deprecated use {@link #on(String, Direction)} instead.
	 * @param key must not be {@literal null} or empty.
	 * @param order must not be {@literal null}.
	 * @return
	 */
	@Deprecated
	public Index on(String key, Order order) {
		return on(key, order.toDirection());
	}

	public Index on(String key, Direction direction) {
		fieldSpec.put(key, direction);
		return this;
	}

	public Index named(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Reject all documents that contain a duplicate value for the indexed field.
	 * 
	 * @return
	 * @see <a href="https://docs.mongodb.org/manual/core/index-unique/">https://docs.mongodb.org/manual/core/index-unique/</a>
	 */
	public Index unique() {
		this.unique = true;
		return this;
	}

	/**
	 * Skip over any document that is missing the indexed field.
	 * 
	 * @return
	 * @see <a href="https://docs.mongodb.org/manual/core/index-sparse/">https://docs.mongodb.org/manual/core/index-sparse/</a>
	 */
	public Index sparse() {
		this.sparse = true;
		return this;
	}

	/**
	 * Build the index in background (non blocking).
	 * 
	 * @return
	 * @since 1.5
	 */
	public Index background() {

		this.background = true;
		return this;
	}

	/**
	 * Specifies TTL in seconds.
	 * 
	 * @param value
	 * @return
	 * @since 1.5
	 */
	public Index expire(long value) {
		return expire(value, TimeUnit.SECONDS);
	}

	/**
	 * Specifies TTL with given {@link TimeUnit}.
	 * 
	 * @param value
	 * @param unit
	 * @return
	 * @since 1.5
	 */
	public Index expire(long value, TimeUnit unit) {

		Assert.notNull(unit, "TimeUnit for expiration must not be null.");
		this.expire = unit.toSeconds(value);
		return this;
	}

	/**
	 * @param duplicates
	 * @return
	 * @see <a href="http://docs.mongodb.org/manual/core/index-creation/#index-creation-duplicate-dropping">http://docs.mongodb.org/manual/core/index-creation/#index-creation-duplicate-dropping</a>
	 */
	public Index unique(Duplicates duplicates) {
		if (duplicates == Duplicates.DROP) {
			this.dropDuplicates = true;
		}
		return unique();
	}

	/**
	 * Only index the documents in a collection that meet a specified {@link IndexFilter filter expression}.
	 *
	 * @param filter can be {@literal null}.
	 * @return
	 * @see <a href=
	 *      "https://docs.mongodb.com/manual/core/index-partial/">https://docs.mongodb.com/manual/core/index-partial/</a>
	 * @since 1.10
	 */
	public Index partial(IndexFilter filter) {

		this.filter = filter;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mongodb.core.index.IndexDefinition#getIndexKeys()
	 */
	public DBObject getIndexKeys() {

		DBObject dbo = new BasicDBObject();

		for (Entry<String, Direction> entry : fieldSpec.entrySet()) {
			dbo.put(entry.getKey(), Direction.ASC.equals(entry.getValue()) ? 1 : -1);
		}

		return dbo;
	}

	public DBObject getIndexOptions() {

		DBObject dbo = new BasicDBObject();
		if (StringUtils.hasText(name)) {
			dbo.put("name", name);
		}
		if (unique) {
			dbo.put("unique", true);
		}
		if (dropDuplicates) {
			dbo.put("dropDups", true);
		}
		if (sparse) {
			dbo.put("sparse", true);
		}
		if (background) {
			dbo.put("background", true);
		}
		if (expire >= 0) {
			dbo.put("expireAfterSeconds", expire);
		}

		if (filter != null) {
			dbo.put("partialFilterExpression", filter.getFilterObject());
		}
		return dbo;
	}

	@Override
	public String toString() {
		return String.format("Index: %s - Options: %s", getIndexKeys(), getIndexOptions());
	}
}
