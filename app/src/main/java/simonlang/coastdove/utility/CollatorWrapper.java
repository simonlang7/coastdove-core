/*  Coast Dove
    Copyright (C) 2016  Simon Lang
    Contact: simon.lang7 at gmail dot com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package simonlang.coastdove.utility;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;

public class CollatorWrapper implements Serializable, Comparator<String> {
    private static final long serialVersionUID = -5418972205681862270L;

    private transient Collator collator;

    public CollatorWrapper() {
        initCollator();
    }

    private void initCollator() {
        if (collator == null)
            this.collator = Collator.getInstance();
    }

    @Override
    public int compare(String lhs, String rhs) {
        initCollator();
        return collator.compare(lhs, rhs);
    }

    private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
    }
}

