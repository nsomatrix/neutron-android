package com.nsomatrix.neutron.appsdb;

import android.database.Cursor;
import androidx.room.EmptyResultSetException;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.RxRoom;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.nsomatrix.neutron.applist.AppItem;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppItemDao_Impl implements AppItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AppItem> __insertionAdapterOfAppItem;

  private final EntityInsertionAdapter<AppItem> __insertionAdapterOfAppItem_1;

  private final EntityDeletionOrUpdateAdapter<AppItem> __deletionAdapterOfAppItem;

  private final EntityDeletionOrUpdateAdapter<AppItem> __updateAdapterOfAppItem;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public AppItemDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAppItem = new EntityInsertionAdapter<AppItem>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `apps` (`id`,`imagePath`,`title`,`author`,`version`,`path`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, AppItem value) {
        stmt.bindLong(1, value.getId());
        if (value.getImagePath() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getImagePath());
        }
        if (value.getTitle() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getTitle());
        }
        if (value.getAuthor() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getAuthor());
        }
        if (value.getVersion() == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.getVersion());
        }
        if (value.getPath() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getPath());
        }
      }
    };
    this.__insertionAdapterOfAppItem_1 = new EntityInsertionAdapter<AppItem>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR IGNORE INTO `apps` (`id`,`imagePath`,`title`,`author`,`version`,`path`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, AppItem value) {
        stmt.bindLong(1, value.getId());
        if (value.getImagePath() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getImagePath());
        }
        if (value.getTitle() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getTitle());
        }
        if (value.getAuthor() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getAuthor());
        }
        if (value.getVersion() == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.getVersion());
        }
        if (value.getPath() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getPath());
        }
      }
    };
    this.__deletionAdapterOfAppItem = new EntityDeletionOrUpdateAdapter<AppItem>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `apps` WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, AppItem value) {
        stmt.bindLong(1, value.getId());
      }
    };
    this.__updateAdapterOfAppItem = new EntityDeletionOrUpdateAdapter<AppItem>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `apps` SET `id` = ?,`imagePath` = ?,`title` = ?,`author` = ?,`version` = ?,`path` = ? WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, AppItem value) {
        stmt.bindLong(1, value.getId());
        if (value.getImagePath() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getImagePath());
        }
        if (value.getTitle() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getTitle());
        }
        if (value.getAuthor() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getAuthor());
        }
        if (value.getVersion() == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.getVersion());
        }
        if (value.getPath() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getPath());
        }
        stmt.bindLong(7, value.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM apps";
        return _query;
      }
    };
  }

  @Override
  public void insert(final AppItem item) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfAppItem.insert(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert(final List<AppItem> items) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfAppItem_1.insert(items);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final AppItem item) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfAppItem.handle(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final List<AppItem> items) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfAppItem.handleMultiple(items);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final AppItem item) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfAppItem.handle(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteAll() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteAll.release(_stmt);
    }
  }

  @Override
  public AppItem get(final String name, final String vendor) {
    final String _sql = "SELECT * FROM apps WHERE title = ? AND author = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (name == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, name);
    }
    _argIndex = 2;
    if (vendor == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, vendor);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "author");
      final int _cursorIndexOfVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "version");
      final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
      final AppItem _result;
      if(_cursor.moveToFirst()) {
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        final String _tmpAuthor;
        if (_cursor.isNull(_cursorIndexOfAuthor)) {
          _tmpAuthor = null;
        } else {
          _tmpAuthor = _cursor.getString(_cursorIndexOfAuthor);
        }
        final String _tmpVersion;
        if (_cursor.isNull(_cursorIndexOfVersion)) {
          _tmpVersion = null;
        } else {
          _tmpVersion = _cursor.getString(_cursorIndexOfVersion);
        }
        final String _tmpPath;
        if (_cursor.isNull(_cursorIndexOfPath)) {
          _tmpPath = null;
        } else {
          _tmpPath = _cursor.getString(_cursorIndexOfPath);
        }
        _result = new AppItem(_tmpPath,_tmpTitle,_tmpAuthor,_tmpVersion);
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _result.setId(_tmpId);
        final String _tmpImagePath;
        if (_cursor.isNull(_cursorIndexOfImagePath)) {
          _tmpImagePath = null;
        } else {
          _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
        }
        _result.setImagePath(_tmpImagePath);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public AppItem get(final int id) {
    final String _sql = "SELECT * FROM apps WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "author");
      final int _cursorIndexOfVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "version");
      final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
      final AppItem _result;
      if(_cursor.moveToFirst()) {
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        final String _tmpAuthor;
        if (_cursor.isNull(_cursorIndexOfAuthor)) {
          _tmpAuthor = null;
        } else {
          _tmpAuthor = _cursor.getString(_cursorIndexOfAuthor);
        }
        final String _tmpVersion;
        if (_cursor.isNull(_cursorIndexOfVersion)) {
          _tmpVersion = null;
        } else {
          _tmpVersion = _cursor.getString(_cursorIndexOfVersion);
        }
        final String _tmpPath;
        if (_cursor.isNull(_cursorIndexOfPath)) {
          _tmpPath = null;
        } else {
          _tmpPath = _cursor.getString(_cursorIndexOfPath);
        }
        _result = new AppItem(_tmpPath,_tmpTitle,_tmpAuthor,_tmpVersion);
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _result.setId(_tmpId);
        final String _tmpImagePath;
        if (_cursor.isNull(_cursorIndexOfImagePath)) {
          _tmpImagePath = null;
        } else {
          _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
        }
        _result.setImagePath(_tmpImagePath);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Flowable<List<AppItem>> getAll(final SupportSQLiteQuery query) {
    final SupportSQLiteQuery _internalQuery = query;
    return RxRoom.createFlowable(__db, false, new String[]{"apps"}, new Callable<List<AppItem>>() {
      @Override
      public List<AppItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _internalQuery, false, null);
        try {
          final List<AppItem> _result = new ArrayList<AppItem>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final AppItem _item;
            _item = __entityCursorConverter_comNsomatrixNeutronApplistAppItem(_cursor);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }
    });
  }

  @Override
  public Single<List<AppItem>> getAllSingle(final SupportSQLiteQuery query) {
    final SupportSQLiteQuery _internalQuery = query;
    return RxRoom.createSingle(new Callable<List<AppItem>>() {
      @Override
      public List<AppItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _internalQuery, false, null);
        try {
          final List<AppItem> _result = new ArrayList<AppItem>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final AppItem _item;
            _item = __entityCursorConverter_comNsomatrixNeutronApplistAppItem(_cursor);
            _result.add(_item);
          }
          if(_result == null) {
            throw new EmptyResultSetException("Query returned empty result set: " + _internalQuery.getSql());
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }
    });
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private AppItem __entityCursorConverter_comNsomatrixNeutronApplistAppItem(Cursor cursor) {
    final AppItem _entity;
    final int _cursorIndexOfId = CursorUtil.getColumnIndex(cursor, "id");
    final int _cursorIndexOfImagePath = CursorUtil.getColumnIndex(cursor, "imagePath");
    final int _cursorIndexOfTitle = CursorUtil.getColumnIndex(cursor, "title");
    final int _cursorIndexOfAuthor = CursorUtil.getColumnIndex(cursor, "author");
    final int _cursorIndexOfVersion = CursorUtil.getColumnIndex(cursor, "version");
    final int _cursorIndexOfPath = CursorUtil.getColumnIndex(cursor, "path");
    final String _tmpTitle;
    if (_cursorIndexOfTitle == -1) {
      _tmpTitle = null;
    } else {
      if (cursor.isNull(_cursorIndexOfTitle)) {
        _tmpTitle = null;
      } else {
        _tmpTitle = cursor.getString(_cursorIndexOfTitle);
      }
    }
    final String _tmpAuthor;
    if (_cursorIndexOfAuthor == -1) {
      _tmpAuthor = null;
    } else {
      if (cursor.isNull(_cursorIndexOfAuthor)) {
        _tmpAuthor = null;
      } else {
        _tmpAuthor = cursor.getString(_cursorIndexOfAuthor);
      }
    }
    final String _tmpVersion;
    if (_cursorIndexOfVersion == -1) {
      _tmpVersion = null;
    } else {
      if (cursor.isNull(_cursorIndexOfVersion)) {
        _tmpVersion = null;
      } else {
        _tmpVersion = cursor.getString(_cursorIndexOfVersion);
      }
    }
    final String _tmpPath;
    if (_cursorIndexOfPath == -1) {
      _tmpPath = null;
    } else {
      if (cursor.isNull(_cursorIndexOfPath)) {
        _tmpPath = null;
      } else {
        _tmpPath = cursor.getString(_cursorIndexOfPath);
      }
    }
    _entity = new AppItem(_tmpPath,_tmpTitle,_tmpAuthor,_tmpVersion);
    if (_cursorIndexOfId != -1) {
      final int _tmpId;
      _tmpId = cursor.getInt(_cursorIndexOfId);
      _entity.setId(_tmpId);
    }
    if (_cursorIndexOfImagePath != -1) {
      final String _tmpImagePath;
      if (cursor.isNull(_cursorIndexOfImagePath)) {
        _tmpImagePath = null;
      } else {
        _tmpImagePath = cursor.getString(_cursorIndexOfImagePath);
      }
      _entity.setImagePath(_tmpImagePath);
    }
    return _entity;
  }
}
