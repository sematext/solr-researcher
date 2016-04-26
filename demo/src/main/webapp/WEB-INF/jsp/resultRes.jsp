<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
  <head>
  </head>

  <body>
    <table>
      <th colspan="2">
        <b>Sematext's ReSearcher component</b>
        <form id="formRes" name="formRes" action="/res/ReSearcherDemoServlet.do">
          <input type="hidden" name="query" id="query" value="tra"/>
          <input type="hidden" name="type" id="type" value="Res"/>
        </form>
      </th>
      <tr><td>&nbsp;</td></tr>
      
      <c:if test="${scResults != null}">
        <tr>
          <td></td>
          <td bgcolor=#c9d7f1>Did you mean : <b><i> ${scResults.bestSuggestion} </i></b>? </td>
        </tr>
        <tr>
          <td></td>
          <td> Such query returns ${scResults.spellcheckedHits} results, like these : </td>
        </tr>

        <c:forEach var="result" items="${scResults.suggestionRows}" varStatus="status">
          <tr>
            <td><i>${status.count}.</td> <td>${result.id}, ${result.foo}, ${result.bar}</i></td>
          </tr>
        </c:forEach>

        <tr>
          <td></td>
          <td>
            <hr size=1 align=left color=#c9d7f1 width=65%>
          </td>
        </tr>
        <tr>
          <td></td>
          <td></td>
        </tr>
      </c:if>
      
      <tr>
        <td></td>
        <td>Results for ${results.query}, found ${results.numFound} documents in ${results.QTime} ms</td>
      </tr>
      <tr><td>&nbsp;</td></tr>

      <c:forEach var="result" items="${results.rows}" varStatus="status">
        <tr>
          <td>${status.count}.</td> <td>${result.id}, ${result.foo}, ${result.bar}</td>
        </tr>
      </c:forEach>
    </table>
  </body>
</html>