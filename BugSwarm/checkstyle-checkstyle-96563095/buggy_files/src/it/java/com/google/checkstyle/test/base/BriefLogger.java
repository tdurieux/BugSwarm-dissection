////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2015 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.google.checkstyle.test.base;

import java.io.OutputStream;

import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

/** A brief logger that only display info about errors. */
class BriefLogger extends DefaultLogger {

    /** Cushion for avoiding StringBuffer.expandCapacity */
    private static final int BUFFER_CUSHION = 12;

    BriefLogger(OutputStream out) {
        super(out, true, out, false, false);
    }

    @Override
    protected String formErrorMessage(AuditEvent event, SeverityLevel severityLevel) {
        final String fileName = event.getFileName();
        final String message = event.getMessage();

        // avoid StringBuffer.expandCapacity
        final int bufLen = fileName.length() + message.length()
            + BUFFER_CUSHION;
        final StringBuilder sb = new StringBuilder(bufLen);

        final char separator = ':';
        sb.append(fileName).append(separator).append(event.getLine());
        if (event.getColumn() > 0) {
            sb.append(separator).append(event.getColumn());
        }
        sb.append(separator).append(' ').append(message);

        return sb.toString();
    }

    @Override
    public void auditStarted(AuditEvent event) { }
}
