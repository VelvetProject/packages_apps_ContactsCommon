/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.contacts.common.test.mocks;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A programmable mock content provider.
 */
public class MockContentProvider extends android.test.mock.MockContentProvider {
    private static final String TAG = "MockContentProvider";

    public static class Query {

        private final Uri mUri;
        private String[] mProjection;
        private String[] mDefaultProjection;
        private String mSelection;
        private String[] mSelectionArgs;
        private String mSortOrder;
        private List<Object> mRows = new ArrayList<>();
        private boolean mAnyProjection;
        private boolean mAnySelection;
        private boolean mAnySortOrder;
        private boolean mAnyNumberOfTimes;

        private boolean mExecuted;

        public Query(Uri uri) {
            mUri = uri;
        }

        @Override
        public String toString() {
            return queryToString(mUri, mProjection, mSelection, mSelectionArgs, mSortOrder);
        }

        public Query withProjection(String... projection) {
            mProjection = projection;
            return this;
        }

        public Query withDefaultProjection(String... projection) {
            mDefaultProjection = projection;
            return this;
        }

        public Query withAnyProjection() {
            mAnyProjection = true;
            return this;
        }

        public Query withSelection(String selection, String... selectionArgs) {
            mSelection = selection;
            mSelectionArgs = selectionArgs;
            return this;
        }

        public Query withAnySelection() {
            mAnySelection = true;
            return this;
        }

        public Query withSortOrder(String sortOrder) {
            mSortOrder = sortOrder;
            return this;
        }

        public Query withAnySortOrder() {
            mAnySortOrder = true;
            return this;
        }

        public Query returnRow(ContentValues values) {
            mRows.add(values);
            return this;
        }

        public Query returnRow(Object... row) {
            mRows.add(row);
            return this;
        }

        public Query returnEmptyCursor() {
            mRows.clear();
            return this;
        }

        public Query anyNumberOfTimes() {
            mAnyNumberOfTimes = true;
            return this;
        }

        public boolean equals(Uri uri, String[] projection, String selection,
                String[] selectionArgs, String sortOrder) {
            if (!uri.equals(mUri)) {
                return false;
            }

            if (!mAnyProjection && !Arrays.equals(projection, mProjection)) {
                return false;
            }

            if (!mAnySelection && !Objects.equals(selection, mSelection)) {
                return false;
            }

            if (!mAnySelection && !Arrays.equals(selectionArgs, mSelectionArgs)) {
                return false;
            }

            if (!mAnySortOrder && !Objects.equals(sortOrder, mSortOrder)) {
                return false;
            }

            return true;
        }

        public Cursor getResult(String[] projection) {
            String[] columnNames;
            if (mAnyProjection) {
                columnNames = projection;
            } else {
                columnNames = mProjection != null ? mProjection : mDefaultProjection;
            }

            MatrixCursor cursor = new MatrixCursor(columnNames);
            for (Object row : mRows) {
                if (row instanceof Object[]) {
                    cursor.addRow((Object[]) row);
                } else {
                    ContentValues values = (ContentValues) row;
                    Object[] columns = new Object[projection.length];
                    for (int i = 0; i < projection.length; i++) {
                        columns[i] = values.get(projection[i]);
                    }
                    cursor.addRow(columns);
                }
            }
            return cursor;
        }
    }

    public static class TypeQuery {
        private final Uri mUri;
        private final String mType;

        public TypeQuery(Uri uri, String type) {
            mUri = uri;
            mType = type;
        }

        public Uri getUri() {
            return mUri;
        }

        public String getType() {
            return mType;
        }

        @Override
        public String toString() {
            return mUri + " --> " + mType;
        }

        public boolean equals(Uri uri) {
            return getUri().equals(uri);
        }
    }

    public static class Insert {
        private final Uri mUri;
        private final ContentValues mContentValues;
        private final Uri mResultUri;
        private boolean mAnyNumberOfTimes;
        private boolean mIsExecuted;

        /**
         * Creates a new Insert to expect.
         *
         * @param uri the uri of the insertion request.
         * @param contentValues the ContentValues to insert.
         * @param resultUri the {@link Uri} for the newly inserted item.
         * @throws NullPointerException if any parameter is {@code null}.
         */
        public Insert(Uri uri, ContentValues contentValues, Uri resultUri) {
            mUri = Preconditions.checkNotNull(uri);
            mContentValues = Preconditions.checkNotNull(contentValues);
            mResultUri = Preconditions.checkNotNull(resultUri);
        }

        /**
         * Causes this insert expectation to be useable for mutliple calls to insert, rather than
         * just one.
         *
         * @return this
         */
        public Insert anyNumberOfTimes() {
            mAnyNumberOfTimes = true;
            return this;
        }

        private boolean equals(Uri uri, ContentValues contentValues) {
            return mUri.equals(uri) && mContentValues.equals(contentValues);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Insert insert = (Insert) o;

            return mUri.equals(insert.mUri) && mContentValues.equals(insert.mContentValues)
                    && mResultUri.equals(insert.mResultUri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mUri, mContentValues, mResultUri);
        }

        @Override
        public String toString() {
            return "Insert{"
                    + "mUri=" + mUri
                    + ", mContentValues=" + mContentValues
                    + ", mResultUri=" + mResultUri
                    +'}';
        }
    }

    private List<Query> mExpectedQueries = new ArrayList<>();
    private Map<Uri, String> mExpectedTypeQueries = Maps.newHashMap();
    private List<Insert> mExpectedInserts = new ArrayList<>();

    @Override
    public boolean onCreate() {
        return true;
    }

    public Query expectQuery(Uri contentUri) {
        Query query = new Query(contentUri);
        mExpectedQueries.add(query);
        return query;
    }

    public void expectTypeQuery(Uri uri, String type) {
        mExpectedTypeQueries.put(uri, type);
    }

    public void expectInsert(Uri contentUri, ContentValues contentValues, Uri resultUri) {
        mExpectedInserts.add(new Insert(contentUri, contentValues, resultUri));
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (mExpectedQueries.isEmpty()) {
            Assert.fail("Unexpected query: Actual:"
                    + queryToString(uri, projection, selection, selectionArgs, sortOrder));
        }

        for (Iterator<Query> iterator = mExpectedQueries.iterator(); iterator.hasNext();) {
            Query query = iterator.next();
            if (query.equals(uri, projection, selection, selectionArgs, sortOrder)) {
                query.mExecuted = true;
                if (!query.mAnyNumberOfTimes) {
                    iterator.remove();
                }
                return query.getResult(projection);
            }
        }

        Assert.fail("Incorrect query. Expected one of: " + mExpectedQueries + ". Actual: " +
                queryToString(uri, projection, selection, selectionArgs, sortOrder));
        return null;
    }

    @Override
    public String getType(Uri uri) {
        if (mExpectedTypeQueries.isEmpty()) {
            Assert.fail("Unexpected getType query: " + uri);
        }

        String mimeType = mExpectedTypeQueries.get(uri);
        if (mimeType != null) {
            return mimeType;
        }

        Assert.fail("Unknown mime type for: " + uri);
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (mExpectedInserts.isEmpty()) {
            Assert.fail("Unexpected insert. Actual: " + insertToString(uri, values));
        }
        for (Iterator<Insert> iterator = mExpectedInserts.iterator(); iterator.hasNext(); ) {
            Insert insert = iterator.next();
            if (insert.equals(uri, values)) {
                insert.mIsExecuted = true;
                if (!insert.mAnyNumberOfTimes) {
                    iterator.remove();
                }
                return insert.mResultUri;
            }
        }

        Assert.fail("Incorrect insert. Expected one of: " + mExpectedInserts + ". Actual: "
                + insertToString(uri, values));
        return null;
    }

    private String insertToString(Uri uri, ContentValues contentValues) {
        return "Insert { mUri=" + uri + ", mContentValues=" + contentValues + '}';
    }

    private static String queryToString(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        StringBuilder sb = new StringBuilder();
        sb.append(uri).append(" ");
        if (projection != null) {
            sb.append(Arrays.toString(projection));
        } else {
            sb.append("[]");
        }
        if (selection != null) {
            sb.append(" selection: '").append(selection).append("'");
            if (selectionArgs != null) {
                sb.append(Arrays.toString(selectionArgs));
            } else {
                sb.append("[]");
            }
        }
        if (sortOrder != null) {
            sb.append(" sort: '").append(sortOrder).append("'");
        }
        return sb.toString();
    }

    public void verify() {
        verifyQueries();
        verifyInserts();
    }

    private void verifyQueries() {
        List<Query> missedQueries = new ArrayList<>();
        for (Query query : mExpectedQueries) {
            if (!query.mExecuted) {
                missedQueries.add(query);
            }
        }
        Assert.assertTrue("Not all expected queries have been called: " + missedQueries,
                missedQueries.isEmpty());
    }

    private void verifyInserts() {
        List<Insert> missedInserts = new ArrayList<>();
        for (Insert insert : mExpectedInserts) {
            if (!insert.mIsExecuted) {
                missedInserts.add(insert);
            }
        }
        Assert.assertTrue("Not all expected inserts have been called: " + missedInserts,
                missedInserts.isEmpty());
    }
}
