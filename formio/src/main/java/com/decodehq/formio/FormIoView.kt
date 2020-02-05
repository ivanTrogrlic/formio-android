package com.decodehq.formio

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * Extends WebView class and adds functionality for rendering FormIo forms.
 */
@SuppressLint("SetJavaScriptEnabled")
public class FormIoView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    public interface SubmissionInterface {
        /**
         * After calling [fetchSubmissionData], submission will be received tru this callback.
         *
         * @param submission json value of form data.
         */
        fun onSubmissionRetrieved(submission: String?)

        /**
         * Whenever user changes something in the form,
         * updated submission will be sent tru this callback.
         *
         * @param submission json value of form data.
         */
        fun onSubmissionChanged(submission: String?)

        /**
         * Whenever user changes something in the form,
         * validity check is run and it's result will be receiver tru this callback.
         *
         * @param isValid true if form is valid, false otherwise.
         */
        fun onValidityChecked(isValid: Boolean)

        /**
         * Whenever user clicks on the input field in the form.
         *
         * @param fieldName Name of the focused field.
         */
        fun onFieldFocused(fieldName: String)
    }

    companion object {
        const val WEB_VIEW_INTERFACE = "formio_interface"
        private const val BASE_URL = "file:///android_asset/"
    }

    init {
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = false
    }

    /**
     * Renders formio in the WebView.
     *
     * @param formIoModel [FormIoModel].
     * @param submissionInterface callback through which submission data is returned
     * when calling [fetchSubmissionData].
     */
    public fun loadForm(formIoModel: FormIoModel, submissionInterface: SubmissionInterface? = null) {
        val html = createHtml(formIoModel)
        addJavascriptInterface(WebViewJavaScriptInterface(submissionInterface), WEB_VIEW_INTERFACE)
        loadDataWithBaseURL(BASE_URL, html, "text/html", "UTF-8", "")
    }

    /**
     * Returns the submission data form the form through [SubmissionInterface.onSubmissionRetrieved].
     */
    public fun fetchSubmissionData() {
        loadUrl("javascript:submitForm()")
    }

    /**
     * Sets the [inputValue] to the FormIo's input field in the component.
     */
    public fun setInputValue(inputName: String, inputValue: String) {
        loadUrl("javascript:setInputValue('$inputName', '$inputValue')")
    }

    /**
     * Builds a HTML with formio form built inside.
     */
    private fun createHtml(formIoModel: FormIoModel): String {
        return "<html> \n" +
                "  <head>\n" +
                "    <link rel='stylesheet' href='formio/app/bootstrap/css/bootstrap.min.css'>\n" +
                "    <link rel='stylesheet' href='formio/dist/formio.full.min.css'>\n" +
                "    <script src='formio/app/jquery/jquery.min.js'></script>\n" +
                "    <script src='formio/app/jquery/jquery.slim.min.js'></script>\n" +
                "    <script src='formio/app/bootstrap/js/bootstrap.bundle.min.js'></script>\n" +
                "    <script src='formio/dist/formio.full.min.js'></script>\n" +
                "" + formioScript(formIoModel) +
                "  </head>\n" +
                "  <body>\n" +
                "    <div id='formio'></div>\n" +
                "  </body>\n" +
                "</html>"
    }

    /**
     * JS script which uses formio javascript library to render custom formio.
     */
    private fun formioScript(formIoModel: FormIoModel): String {
        return "     <script type='text/javascript'>\n" +
                "    let formVar;\n" +
                "    window.onload = function() {\n" +
                "         Formio.createForm(document.getElementById('formio'),\n" +
                "           ${formIoModel.formConfig},\n" +
                "               {readOnly: ${formIoModel.readOnly}})\n" +
                "               .then(function(form) {\n" +
                "                   formVar = form;\n" +
                "               " + observeFormChanges() +
                "               " + attachFocusListener() +
                "               " + preFillForm(formIoModel.formData) +
                "               })\n" +
                "    }\n" +
                "" + declareSubmitFormFun() +
                "" + declareSetInputValueFun() +
                "    </script>\n"
    }

    /**
     * Attaches to FormIo's change callback and pushes the changed submission values
     * to [WebViewJavaScriptInterface.submissionChanged] every time user changes something in
     * the form.
     */
    private fun observeFormChanges(): String {
        return "    form.on('change', (component, value) => {\n" +
                "       let jsonData = JSON.stringify(form.submission.data);\n" +
                "       $WEB_VIEW_INTERFACE.submissionChanged(jsonData)\n" +
                "       let isValid = form.checkValidity(form.submission.data);\n" +
                "       $WEB_VIEW_INTERFACE.validityChecked(isValid);\n" +
                "   });\n"
    }

    /**
     * Attaches a focus listener to every input field and pushes it to
     * [WebViewJavaScriptInterface.fieldFocused].
     */
    private fun attachFocusListener(): String {
        return "    $('#formio').on('focusin', (event) => {\n" +
                "       var target = $(event.target);\n" +
                "       if (target.is('input') || target.is('textarea')) {\n" +
                "           $WEB_VIEW_INTERFACE.fieldFocused(target.prop('name'));\n" +
                "       }\n" +
                "   });\n"
    }

    /**
     * Returns a function which pre-fills the formio with provided data if available.
     */
    private fun preFillForm(formData: String?): String {
        val data = if (formData == null || formData.isBlank() || formData.isEmpty()) {
            "{}" // from.submission data doesn't accept empty string.
        } else formData

        return "    form.nosubmit = true;\n" +
                "   form.submission = {\n" +
                "       data:\n" +
                "           $data\n" +
                "   };\n" +
                "   formVar.getAllComponents().forEach(component => {\n" +
                "       component.triggerChange();\n" +
                "   });\n"
    }

    /**
     * Declares a JS submitForm function which can be called from the outside.
     * [fetchSubmissionData] is a proxy method for this function when called from the outside.
     */
    private fun declareSubmitFormFun(): String {
        return "    function submitForm(){\n" +
                "       let jsonData = JSON.stringify(formVar.submission.data);\n" +
                "       $WEB_VIEW_INTERFACE.submissionData(jsonData);\n" +
                "    }\n"
    }

    /**
     * Declares a JS setInputValue function which can be called from the outside.
     * [setInputValue] is a proxy method for this function when called from the outside.
     */
    private fun declareSetInputValueFun(): String {
        return "    function setInputValue(inputName, inputValue){\n" +
                "       formVar.getAllComponents().forEach(component => {\n" +
                "               component.inputs.forEach(input => {\n" +
                "                   if (input.name == inputName){\n" +
                "                       component.setValue(inputValue, false);\n" +
                "                   }\n" +
                "               });\n" +
                "       });\n" +
                "    }\n"
    }

    private class WebViewJavaScriptInterface internal
    constructor(val submissionInterface: SubmissionInterface?) {
        @JavascriptInterface
        fun submissionData(submissionData: String?) {
            submissionInterface?.onSubmissionRetrieved(submissionData)
        }

        @JavascriptInterface
        fun submissionChanged(submissionData: String?) {
            submissionInterface?.onSubmissionChanged(submissionData)
        }

        @JavascriptInterface
        fun validityChecked(isValid: Boolean) {
            submissionInterface?.onValidityChecked(isValid)
        }

        @JavascriptInterface
        fun fieldFocused(fieldName: String) {
            submissionInterface?.onFieldFocused(fieldName)
        }
    }
}

/**
 * Wrapper model with data required for FormIo form rendering.
 *
 * @param formConfig JSON value of form config.
 * @param formData JSON value of form data if available.
 * @param readOnly makes form editable or not. It's editable by default.
 */
public data class FormIoModel(val formConfig: String?,
                              val formData: String? = "",
                              val readOnly: Boolean = false)
