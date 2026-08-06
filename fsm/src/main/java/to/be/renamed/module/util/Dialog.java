/*
 * BasicWorkflows Module
 * %%
 * Copyright (C) 2012 - 2025 Crownpeak Technology GmbH - https://www.crownpeak.com
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
 */
package to.be.renamed.module.util;

import to.be.renamed.module.review.formatter.ReviewTheme;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.agency.SpecialistsBroker;
import de.espirit.firstspirit.ui.operations.RequestOperation;
import de.espirit.firstspirit.webedit.server.ClientScriptOperation;

import java.io.Serializable;

/**
 * The type Dialog.
 */
public class Dialog {

    private static final String LOADING_OVERLAY_ID = "bw-ai-review-loading";

    private final SpecialistsBroker context;

    /**
     * Instantiates a new Dialog.
     *
     * @param broker the broker
     */
    public Dialog(SpecialistsBroker broker) {
        if (broker == null) {
            throw new IllegalArgumentException("SpecialistsBroker is null");
        }
        this.context = broker;
    }


    /**
     * Show dialog.
     *
     * @param kind    the kind of request operation
     * @param type    the type of question if the kind is question
     * @param title   the title of the request
     * @param message the message of the request
     * @return true for positive answer such as yes or ok, false otherwise
     */
    public boolean showDialog(RequestOperation.Kind kind, QuestionType type, String title, String message) {
        if (kind == null) {
            throw new IllegalArgumentException("RequestOperation.Kind is null");
        }
        if (type == null) {
            throw new IllegalArgumentException("QuestionType is null");
        }
        if (title == null) {
            throw new IllegalArgumentException("title is null");
        }
        if (message == null) {
            throw new IllegalArgumentException("message is null");
        }
        boolean result;
        try {
            RequestOperation requestOperation = loadRequestOperation();
            if (requestOperation != null) {
                requestOperation.setTitle(title);
                requestOperation.setKind(kind);
                RequestOperation.Answer positiveAnswer = getPositiveAnswerFrom(type, requestOperation);
                final RequestOperation.Answer answer = requestOperation.perform(message);
                result = answer != null && answer.equals(positiveAnswer);
            } else {
                result = false;
            }
        } catch (Exception e) { //NOSONAR
            result = false;
            Logging.logError("Show dialog (" + kind.name() + "/" + type.name() + ") failed: " + e.getMessage(), e, getClass());

        }
        return result;
    }

    private static RequestOperation.Answer getPositiveAnswerFrom(final QuestionType type, final RequestOperation requestOperation) {
        RequestOperation.Answer positiveAnswer = null;
        switch (type) {
            case YES_NO:
                positiveAnswer = requestOperation.addYes();
                requestOperation.addNo();
                break;
            case OK_CANCEL:
                positiveAnswer = requestOperation.addOk();
                requestOperation.addCancel();
                break;
            default:
                break;
        }
        return positiveAnswer;
    }

    private RequestOperation loadRequestOperation() {
        OperationAgent operationAgent = context.requireSpecialist(OperationAgent.TYPE);
        return operationAgent.getOperation(RequestOperation.TYPE);
    }

    /**
     * Show info dialog.
     *
     * @param title   the title
     * @param message the info message
     */

    public void showInfo(String title, String message) {
        showDialog(RequestOperation.Kind.INFO, QuestionType.NONE, title, message);
    }

    /**
     * Show error dialog.
     *
     * @param title   the title
     * @param message the error message
     */
    public void showError(String title, String message) {
        showDialog(RequestOperation.Kind.ERROR, QuestionType.NONE, title, message);
    }

    /**
     * Show question dialog.
     *
     * @param type     the type of question
     * @param title    the title of the request
     * @param question the question message of the request
     * @return true for positive answer such as yes or ok, false otherwise
     */
    public boolean showQuestion(QuestionType type, String title, String question) {
        return showDialog(RequestOperation.Kind.QUESTION, type, title, question);
    }

    /**
     * Show question dialog with custom button labels.
     *
     * @param title           the title of the request
     * @param message         the message of the request
     * @param positiveLabel   the label for the positive button
     * @param negativeLabel   the label for the negative button
     * @return true if user clicked the positive button, false otherwise
     */
    public boolean showCustomQuestion(String title, String message, String positiveLabel, String negativeLabel) {
        try {
            RequestOperation requestOperation = loadRequestOperation();
            if (requestOperation != null) {
                requestOperation.setTitle(title);
                requestOperation.setKind(RequestOperation.Kind.QUESTION);
                final RequestOperation.Answer positiveAnswer = requestOperation.addAnswer(positiveLabel);
                requestOperation.addAnswer(negativeLabel);
                final RequestOperation.Answer answer = requestOperation.perform(message);
                return answer != null && answer.equals(positiveAnswer);
            }
        } catch (Exception e) {
            Logging.logError("showCustomQuestion failed: " + e.getMessage(), e, getClass());
        }
        return false;
    }

    /**
     * Show an HTML info dialog via ClientScriptOperation (for ContentCreator).
     *
     * @param title       the dialog title
     * @param htmlContent the HTML content to display
     */
    public void showHtmlInfo(final String title, final String htmlContent) {
        showHtmlDialog(title, htmlContent, null, null);
    }

    /**
     * Show an HTML question dialog via ClientScriptOperation (for ContentCreator).
     * Displays HTML content with Yes/No buttons and returns the user's choice.
     *
     * @param title       the dialog title
     * @param htmlContent the HTML content to display
     * @param yesLabel    the label for the positive button
     * @param noLabel     the label for the negative button
     * @return true if user clicked Yes, false otherwise
     */
    public boolean showHtmlQuestion(final String title, final String htmlContent,
                                    final String yesLabel, final String noLabel) {
        return showHtmlDialog(title, htmlContent, yesLabel, noLabel);
    }

    /**
     * Injects a non-blocking loading overlay into the ContentCreator DOM.
     * Uses a synchronous {@code ClientScriptOperation} so it fires and returns immediately,
     * allowing the server to proceed to the AI call while the overlay is visible.
     * The overlay auto-removes itself after 180 seconds as a safety fallback.
     *
     * @param title   the overlay header (typically the AI review title)
     * @param message the loading message shown below the spinner
     * @since 1.6.0
     */
    public void showLoadingOverlay(final String title, final String message) {
        try {
            final OperationAgent operationAgent = context.requireSpecialist(OperationAgent.TYPE);
            final ClientScriptOperation scriptOp = operationAgent.getOperation(ClientScriptOperation.TYPE);
            if (scriptOp != null) {
                scriptOp.perform(buildLoadingShowScript(title, message), false);
            }
        } catch (Exception e) {
            Logging.logError("showLoadingOverlay failed: " + e.getMessage(), e, getClass());
        }
    }

    /**
     * Removes the loading overlay injected by {@link #showLoadingOverlay}.
     * Called defensively in non-modal exit paths to ensure the overlay never lingers.
     *
     * @since 1.6.0
     */
    public void hideLoadingOverlay() {
        try {
            final OperationAgent operationAgent = context.requireSpecialist(OperationAgent.TYPE);
            final ClientScriptOperation scriptOp = operationAgent.getOperation(ClientScriptOperation.TYPE);
            if (scriptOp != null) {
                scriptOp.perform(buildLoadingHideScript(), false);
            }
        } catch (Exception e) {
            Logging.logError("hideLoadingOverlay failed: " + e.getMessage(), e, getClass());
        }
    }

    static String buildLoadingShowScript(final String title, final String message) {
        final String escapedTitle = escapeJs(title);
        final String escapedMsg = escapeJs(message);
        final String msgElId = LOADING_OVERLAY_ID + "-msg";
        return "function() {\n" +
               "  var existing = document.getElementById('" + LOADING_OVERLAY_ID + "');\n" +
               "  if (existing) {\n" +
               "    var msgEl = document.getElementById('" + msgElId + "');\n" +
               "    if (msgEl) { msgEl.textContent = '" + escapedMsg + "'; }\n" +
               "    return null;\n" +
               "  }\n" +
               "  var st = document.createElement('style');\n" +
               "  st.textContent = '@keyframes bw-spin{0%{transform:rotate(0deg)}100%{transform:rotate(360deg)}}';\n" +
               "  document.head.appendChild(st);\n" +
               "  var overlay = document.createElement('div');\n" +
               "  overlay.id = '" + LOADING_OVERLAY_ID + "';\n" +
               "  overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.45);z-index:99998;display:flex;align-items:center;justify-content:center;font-family:\"Open Sans\",Arial,Helvetica,sans-serif';\n" +
               "  var box = document.createElement('div');\n" +
               "  box.style.cssText = 'background:#fff;border-radius:4px;box-shadow:0 4px 16px rgba(0,0,0,0.3);min-width:300px;max-width:420px;overflow:hidden';\n" +
               "  var hdr = document.createElement('div');\n" +
               "  hdr.style.cssText = 'background:" + ReviewTheme.HEADER_COLOR + ";padding:14px 20px;display:flex;align-items:center;gap:12px';\n" +
               "  var spinner = document.createElement('div');\n" +
               "  spinner.style.cssText = 'width:18px;height:18px;border:2px solid rgba(255,255,255,0.3);border-top:2px solid #fff;border-radius:50%;animation:bw-spin 0.8s linear infinite;flex-shrink:0';\n" +
               "  var titleEl = document.createElement('div');\n" +
               "  titleEl.style.cssText = 'font-size:14px;font-weight:600;color:#fff;letter-spacing:0.01em';\n" +
               "  titleEl.textContent = '" + escapedTitle + "';\n" +
               "  hdr.appendChild(spinner);\n" +
               "  hdr.appendChild(titleEl);\n" +
               "  var bdy = document.createElement('div');\n" +
               "  bdy.style.cssText = 'padding:16px 20px';\n" +
               "  var msgEl = document.createElement('div');\n" +
               "  msgEl.id = '" + msgElId + "';\n" +
               "  msgEl.style.cssText = 'font-size:13px;color:#555;line-height:1.5';\n" +
               "  msgEl.textContent = '" + escapedMsg + "';\n" +
               "  bdy.appendChild(msgEl);\n" +
               "  box.appendChild(hdr);\n" +
               "  box.appendChild(bdy);\n" +
               "  overlay.appendChild(box);\n" +
               "  document.body.appendChild(overlay);\n" +
               "  setTimeout(function() {\n" +
               "    var el = document.getElementById('" + LOADING_OVERLAY_ID + "');\n" +
               "    if (el) { el.parentNode.removeChild(el); }\n" +
               "  }, 180000);\n" +
               "  return null;\n" +
               "}\n";
    }

    static String buildLoadingHideScript() {
        return "function() {\n" +
               "  var el = document.getElementById('" + LOADING_OVERLAY_ID + "');\n" +
               "  if (el) { el.parentNode.removeChild(el); }\n" +
               "  return null;\n" +
               "}\n";
    }

    private boolean showHtmlDialog(final String title, final String htmlContent,
                                   final String yesLabel, final String noLabel) {
        try {
            final OperationAgent operationAgent = context.requireSpecialist(OperationAgent.TYPE);
            final ClientScriptOperation scriptOp = operationAgent.getOperation(ClientScriptOperation.TYPE);

            final boolean isQuestion = yesLabel != null && noLabel != null;
            final String escapedTitle = escapeJs(title);
            final String escapedHtml = escapeJs(htmlContent);
            final String escapedYes = isQuestion ? escapeJs(yesLabel) : "";
            final String escapedNo = isQuestion ? escapeJs(noLabel) : "";

            final String script = buildModalScript(escapedTitle, escapedHtml,
                                                   isQuestion, escapedYes, escapedNo);

            assert scriptOp != null;
            final Serializable result = scriptOp.perform(script, true);
            return "true".equals(String.valueOf(result));
        } catch (Exception e) {
            Logging.logError("showHtmlDialog failed: " + e.getMessage(), e, getClass());
            return false;
        }
    }

    private static String buildModalScript(final String title, final String html,
                                           final boolean isQuestion,
                                           final String yesLabel, final String noLabel) {
        // Fixed height + border-box so the primary (no border) and cancel (1px border) buttons are
        // exactly the same size, matching the ~42px ContentCreator buttons. inline-flex centers the
        // glyph + label vertically regardless of the border.
        final String btnBase = "height:42px;box-sizing:border-box;padding:0 20px;border-radius:6px;cursor:pointer;"
                + "font-size:14px;font-family:'Open Sans',Arial,Helvetica,sans-serif;"
                + "display:inline-flex;align-items:center;justify-content:center";
        // ContentCreator native button styling: solid-blue primary, light-blue outlined cancel.
        final String primaryStyle = btnBase + ";border:none;background:" + ReviewTheme.BUTTON_PRIMARY_BG + ";color:#fff";
        final String cancelStyle = btnBase + ";border:1px solid " + ReviewTheme.BUTTON_CANCEL_BORDER
                + ";background:" + ReviewTheme.BUTTON_CANCEL_BG + ";color:" + ReviewTheme.BUTTON_CANCEL_TEXT;
        final String buttons;
        if (isQuestion) {
            // Match ContentCreator layout: primary action on the left, cancel on the right.
            buttons =
                "<div style=\"display:flex;justify-content:space-between;gap:8px\">" +
                "<button id=\"bw-modal-yes\" style=\"" + primaryStyle + "\">▷ " + yesLabel + "</button>" +
                "<button id=\"bw-modal-no\" style=\"" + cancelStyle + "\">✕ " + noLabel + "</button>" +
                "</div>";
        } else {
            buttons =
                "<div style=\"display:flex;justify-content:flex-end\">" +
                "<button id=\"bw-modal-yes\" style=\"" + primaryStyle + "\">▷ OK</button>" +
                "</div>";
        }

        return "function(callback) {\n" +
               "  var loadingEl = document.getElementById('" + LOADING_OVERLAY_ID + "');\n" +
               "  if (loadingEl) { loadingEl.parentNode.removeChild(loadingEl); }\n" +
               "  var overlay = document.createElement('div');\n" +
               "  overlay.id = 'bw-modal-overlay';\n" +
               "  overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.45);z-index:99999;display:flex;align-items:center;justify-content:center;font-family:\"Open Sans\",Arial,Helvetica,sans-serif';\n" +
               "  var modal = document.createElement('div');\n" +
               "  modal.style.cssText = 'background:#fff;border-radius:4px;box-shadow:0 4px 16px rgba(0,0,0,0.3);max-width:700px;width:90%;max-height:85vh;display:flex;flex-direction:column;overflow:hidden';\n" +
               "  var header = document.createElement('div');\n" +
               "  header.style.cssText = 'background:" + ReviewTheme.HEADER_COLOR + ";padding:14px 20px;font-size:15px;font-weight:600;color:#fff;flex-shrink:0;letter-spacing:0.01em';\n" +
               "  header.textContent = '" + title + "';\n" +
               "  var body = document.createElement('div');\n" +
               "  body.style.cssText = 'padding:20px;overflow-y:auto;flex:1;font-size:13px;color:#333;line-height:1.5';\n" +
               "  body.innerHTML = '" + html + "';\n" +
               "  var footer = document.createElement('div');\n" +
               "  footer.style.cssText = 'padding:12px 20px;border-top:1px solid #e0e0e0;background:#f8f8f8;flex-shrink:0';\n" +
               "  footer.innerHTML = '" + escapeJs(buttons) + "';\n" +
               "  modal.appendChild(header);\n" +
               "  modal.appendChild(body);\n" +
               "  modal.appendChild(footer);\n" +
               "  overlay.appendChild(modal);\n" +
               "  document.body.appendChild(overlay);\n" +
               "  var close = function(val) {\n" +
               "    document.body.removeChild(overlay);\n" +
               "    callback(val);\n" +
               "  };\n" +
               "  footer.querySelector('#bw-modal-yes').addEventListener('click', function() { close('true'); });\n" +
               (isQuestion
                ? "  footer.querySelector('#bw-modal-no').addEventListener('click', function() { close('false'); });\n"
                : "") +
               "}\n";
    }

    private static String escapeJs(final String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
            .replace("</", "<\\/");
    }

    /**
     * The enum Question type.
     */
    public enum QuestionType {

        /**
         * The NONE Type.
         */
        NONE,

        /**
         * The YES_NO Type.
         */
        YES_NO,

        /**
         * The OK_CANCEL Type.
         */
        OK_CANCEL
    }

}

