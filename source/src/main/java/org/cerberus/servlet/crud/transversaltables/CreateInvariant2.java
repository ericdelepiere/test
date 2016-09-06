/* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This file is part of Cerberus.
 *
 * Cerberus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cerberus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cerberus.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cerberus.servlet.crud.transversaltables;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cerberus.crud.entity.Invariant;
import org.cerberus.crud.entity.MessageEvent;
import org.cerberus.crud.factory.impl.FactoryInvariant;
import org.cerberus.enums.MessageEventEnum;
import org.cerberus.exception.CerberusException;
import org.cerberus.crud.factory.IFactoryInvariant;
import org.cerberus.crud.factory.IFactoryLogEvent;
import org.cerberus.crud.factory.impl.FactoryLogEvent;
import org.cerberus.crud.service.IInvariantService;
import org.cerberus.crud.service.ILogEventService;
import org.cerberus.crud.service.impl.LogEventService;
import org.cerberus.crud.service.impl.UserService;
import org.cerberus.util.ParameterParserUtil;
import org.cerberus.util.StringUtil;
import org.cerberus.util.answer.Answer;
import org.json.JSONException;
import org.json.JSONObject;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author bcivel
 */
public class CreateInvariant2 extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, CerberusException, JSONException {
        JSONObject jsonResponse = new JSONObject();
        Answer ans = new Answer();
        MessageEvent msg = new MessageEvent(MessageEventEnum.DATA_OPERATION_ERROR_UNEXPECTED);
        msg.setDescription(msg.getDescription().replace("%DESCRIPTION%", ""));
        ans.setResultMessage(msg);
        PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
        String charset = request.getCharacterEncoding();

        String id = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter("Idname"), "", charset);
        String value = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter("Value"), "", charset);
        String description = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter("Description"), "", charset);
        String veryShortDescField = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter("VeryShortDesc"), "", charset);
        String gp1 = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter("gp1"), "", charset);
        String gp2 = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter("gp2"), "", charset);
        String gp3 = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter("gp3"), "", charset);

        Integer sort = 10;
        boolean sort_error = false;
        try {
            if (request.getParameter("Sort") != null && !request.getParameter("Sort").equals("")) {
                sort = Integer.valueOf(policy.sanitize(request.getParameter("Sort")));
            }
        } catch (Exception ex) {
            sort_error = true;
        }

        boolean userHasPermissions = request.isUserInRole("Administrator");

        /**
         * Checking all constrains before calling the services.
         */
        if (StringUtil.isNullOrEmpty(id)) {
            msg = new MessageEvent(MessageEventEnum.DATA_OPERATION_ERROR_EXPECTED);
            msg.setDescription(msg.getDescription().replace("%ITEM%", "Invariant")
                    .replace("%OPERATION%", "Create")
                    .replace("%REASON%", "Invariant name is missing!"));
            ans.setResultMessage(msg);
        } else if (sort_error) {
            msg = new MessageEvent(MessageEventEnum.DATA_OPERATION_ERROR_EXPECTED);
            msg.setDescription(msg.getDescription().replace("%ITEM%", "Invariant")
                    .replace("%OPERATION%", "Create")
                    .replace("%REASON%", "Could not manage to convert sort to an integer value!"));
            ans.setResultMessage(msg);
        } else if (!userHasPermissions) {
            msg = new MessageEvent(MessageEventEnum.DATA_OPERATION_ERROR_EXPECTED);
            msg.setDescription(msg.getDescription().replace("%ITEM%", "Invariant")
                    .replace("%OPERATION%", "Create")
                    .replace("%REASON%", "You don't have the right to do that"));
            ans.setResultMessage(msg);
        } else {
            /**
             * All data seems cleans so we can call the services.
             */

            ApplicationContext appContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
            IInvariantService invariantService = appContext.getBean(IInvariantService.class);
            IFactoryInvariant factoryInvariant = appContext.getBean(IFactoryInvariant.class);
            Invariant invariantData = factoryInvariant.create(id, value, sort, description, veryShortDescField, gp1, gp2, gp3);

            invariantService.createInvariant(invariantData);

            /**
             * Object updated. Adding Log entry.
             */
            ILogEventService logEventService = appContext.getBean(LogEventService.class);
            logEventService.createPrivateCalls("/CreateInvariant2", "UPDATE", "Create Invariant : ['" + id + "']", request);

        }

        /**
         * Formating and returning the json result.
         */
        jsonResponse.put("messageType", ans.getResultMessage().getMessage().getCodeString());
        jsonResponse.put("message", ans.getResultMessage().getDescription());

        response.getWriter().print(jsonResponse);
        response.getWriter().flush();
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (CerberusException ex) {
            Logger.getLogger(CreateInvariant.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(CreateInvariant.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (CerberusException ex) {
            Logger.getLogger(CreateInvariant.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(CreateInvariant.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
