/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.qpython.qsl4a.qsl4a.future;

import android.content.Context;
import android.content.Intent;

import org.qpython.qsl4a.R;
import org.qpython.qsl4a.qsl4a.Constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FutureActivityTaskExecutor {

  private final Context mContext;
  private final Map<Integer, FutureActivityTask<?>> mTaskMap =
      new ConcurrentHashMap<Integer, FutureActivityTask<?>>();
  private final AtomicInteger mIdGenerator = new AtomicInteger(0);
  private final int intentFlags = Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;

  public FutureActivityTaskExecutor(Context context) {
    mContext = context;
  }

  public void execute(FutureActivityTask<?> task) {
    int id = mIdGenerator.incrementAndGet();
    mTaskMap.put(id, task);
    Intent helper = new Intent(mContext, FutureActivity.class);
    helper.putExtra(Constants.EXTRA_TASK_ID, id);
    helper.setFlags(intentFlags);
    mContext.startActivity(helper);
  }

  public void execute(FutureActivityTask<?> task,int flags) {
    int id = mIdGenerator.incrementAndGet();
    mTaskMap.put(id, task);
    Intent helper = new Intent(mContext, FutureActivity.class);
    helper.putExtra(Constants.EXTRA_TASK_ID, id);
    helper.setFlags(flags);
    mContext.startActivity(helper);
  }

  public FutureActivityTask<?> getTask(int id) {
    return mTaskMap.remove(id);
  }

}
