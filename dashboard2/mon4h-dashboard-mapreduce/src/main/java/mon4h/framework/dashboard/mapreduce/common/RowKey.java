// This file is part of OpenTSDB.
// Copyright (C) 2010-2012  The OpenTSDB Authors.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.
package mon4h.framework.dashboard.mapreduce.common;

import java.util.Arrays;

/** Helper functions to deal with the row key. */
final class RowKey {

  private RowKey() {
    // Can't create instances of this utility class.
  }

  /**
   * Extracts the name of the metric ID contained in a row key.
   * @param tsdb The TSDB to use.
   * @param row The actual row key.
   * @return The name of the metric.
   */
  static String metricName(final TSDB tsdb, final byte[] row) {
    final byte[] id = Arrays.copyOfRange(row, 0, UniqueIds.metrics().width());
    return UniqueIds.metrics().getName(id);
  }

}
