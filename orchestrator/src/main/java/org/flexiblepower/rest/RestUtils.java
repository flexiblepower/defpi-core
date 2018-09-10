/*-
 * #%L
 * dEF-Pi REST Orchestrator
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
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
 * #L%
 */
package org.flexiblepower.rest;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * RestUtils
 *
 * @version 0.1
 * @since Aug 3, 2018
 */
class RestUtils {

    /**
     * Filter a list of content by checking if one of its properties is present in an array. It will change the existing
     * collection in place, i.e. it will return by argument.
     *
     * @param content The collection we are filtering
     * @param function The function to perform on elements of the collection to get the value to filter on
     * @param filter The map containing the filter operations
     * @param filterKey The key to filter on (if it is not present in the filter, this function will do nothing
     */
    static <T> void filterMultiContent(final Iterable<T> content,
            final Function<T, Object> function,
            final Map<String, Object> filter,
            final String filterKey) {
        if (!filter.containsKey(filterKey)) {
            return;
        }

        @SuppressWarnings("unchecked")
        final List<String> filterValues = (List<String>) filter.get(filterKey);

        final Iterator<T> it = content.iterator();
        while (it.hasNext()) {
            final String property = function.apply(it.next()).toString();
            if (!filterValues.contains(property)) {
                it.remove();
            }
        }
    }

    /**
     * Filter a list of content by checking if one of its properties equals a particular filter OR matches a regex. It
     * will change the existing collection in place, i.e. it will return by argument.
     *
     * @param content The collection we are filtering
     * @param function The function to perform on elements of the collection to get the value to filter on
     * @param filter The map containing the filter operations
     * @param filterKey The key to filter on (if it is not present in the filter, this function will do nothing
     */
    static <T> void filterContent(final Iterable<T> content,
            final Function<T, Object> function,
            final Map<String, Object> filter,
            final String filterKey) {
        if (!filter.containsKey(filterKey)) {
            return;
        }

        final String filterValue = filter.get(filterKey).toString();

        final Iterator<T> it = content.iterator();

        if ((filterValue.charAt(0) == '/') && (filterValue.charAt(filterValue.length() - 1) == '/')) {
            // It is a regex, compile the pattern and filter out non-matching elements
            final Pattern pattern = Pattern.compile(filterValue.substring(1, filterValue.length() - 1));
            while (it.hasNext()) {
                final Object value = function.apply(it.next());
                if ((value == null) || !pattern.matcher(value.toString()).find()) {
                    it.remove();
                }
            }
        } else {
            // It is a just some value, compare for equality
            while (it.hasNext()) {
                final Object value = function.apply(it.next());
                if ((value == null) || !filterValue.equals(value.toString())) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Order the elements of some content according to a comparator which we can lookup in a map. It will change the
     * existing collection in place, i.e. it will return by argument.
     *
     * @param content The collection of elements
     * @param sortMap A map of comparators to select one out of
     * @param sortField The key of the map that corresponds to the comparator to use
     * @param sortDir A string that if it equals "DESC", it will reverse the order of the elements
     */
    static <T, U extends Comparable<?>> void orderContent(final List<T> content,
            final Map<String, Function<T, U>> sortMap,
            final String sortField,
            final String sortDir) {
        final Function<T, U> keyExtractor = sortMap.getOrDefault(sortField, sortMap.get("default"));

        @SuppressWarnings("unchecked")
        final Comparator<T> comparator = (o1, o2) -> {
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            final Comparable<U> val1 = (Comparable<U>) keyExtractor.apply(o1);
            final Comparable<U> val2 = (Comparable<U>) keyExtractor.apply(o2);

            if (val1 == null) {
                return -1;
            }
            if (val2 == null) {
                return 1;
            }
            return val1.compareTo((U) val2);
        };

        content.sort(comparator);

        // Order the sorting if necessary
        if ("DESC".equals(sortDir)) {
            Collections.reverse(content);
        }

    }

    /**
     * Get a sublist of the content based on the number of elements per page, and the page we are currently on
     *
     * @param content The full list
     * @param page The page number to return
     * @param perPage The number of elements per page
     * @return The sublist that contains at most <i>perPage</i> elements
     */
    static <T> List<T> paginate(final List<T> content, final int page, final int perPage) {
        if ((page == 0) || (perPage == 0)) {
            // A special case to just return ALL content
            return content;
        }
        return content.subList(Math.min(content.size(), (page - 1) * perPage),
                Math.min(content.size(), page * perPage));
    }

}
