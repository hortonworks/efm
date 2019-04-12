/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 *  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 *  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 *  properly licensed third party, you do not have any rights to this code.
 *
 *  If this code is provided to you under the terms of the AGPLv3:
 *   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *       LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *       FROM OR RELATED TO THE CODE; AND
 *   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *       DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *       TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *       UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */

package com.cloudera.cem.efm.validation;

import com.cloudera.cem.efm.model.extension.ExpressionLanguageScope;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;

public class ExpressionLanguageUtil {

    public static boolean isExpressionLanguagePresent(final PropertyDescriptor descriptor, final String value) {
        final ExpressionLanguageScope scope = descriptor.getExpressionLanguageScope();
        if (scope == null || scope == ExpressionLanguageScope.NONE) {
            return false;
        }

        char lastChar = 0;
        int embeddedCount = 0;
        int expressionStart = -1;
        boolean oddDollarCount = false;
        int backslashCount = 0;

        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);

            if (expressionStart > -1 && (c == '\'' || c == '"') && (lastChar != '\\' || backslashCount % 2 == 0)) {
                final int endQuoteIndex = findEndQuoteChar(value, i);
                if (endQuoteIndex < 0) {
                    break;
                }

                i = endQuoteIndex;
                continue;
            }

            if (c == '{') {
                if (oddDollarCount && lastChar == '$') {
                    if (embeddedCount == 0) {
                        expressionStart = i - 1;
                    }
                }

                // Keep track of the number of opening curly braces that we are embedded within,
                // if we are within an Expression. If we are outside of an Expression, we can just ignore
                // curly braces. This allows us to ignore the first character if the value is something
                // like: { ${abc} }
                // However, we will count the curly braces if we have something like: ${ $${abc} }
                if (expressionStart > -1) {
                    embeddedCount++;
                }
            } else if (c == '}') {
                if (embeddedCount <= 0) {
                    continue;
                }

                if (--embeddedCount == 0) {
                    if (expressionStart > -1) {
                        // ended expression. EL is present
                        return true;
                    }

                    expressionStart = -1;
                }
            } else if (c == '$') {
                oddDollarCount = !oddDollarCount;
            } else if (c == '\\') {
                backslashCount++;
            } else {
                oddDollarCount = false;
            }

            lastChar = c;
        }

        return false;
    }

    private static int findEndQuoteChar(final String value, final int quoteStart) {
        final char quoteChar = value.charAt(quoteStart);

        int backslashCount = 0;
        char lastChar = 0;
        for (int i = quoteStart + 1; i < value.length(); i++) {
            final char c = value.charAt(i);

            if (c == '\\') {
                backslashCount++;
            } else if (c == quoteChar && (backslashCount % 2 == 0 || lastChar != '\\')) {
                return i;
            }

            lastChar = c;
        }

        return -1;
    }
}
