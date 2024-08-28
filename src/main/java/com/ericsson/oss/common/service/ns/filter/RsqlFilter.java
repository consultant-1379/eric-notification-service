/*******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.oss.common.service.ns.filter;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.minidev.json.JSONArray;

/**
 * Class to manage the filter attribute of a subscription. Filter is stored and can be used to check an event payload.
 */
public class RsqlFilter {

  /**
   * The root of the evaluation tree
   */
  private Node root;

  /**
   * Class to visit the evaluation tree of the filter.
   */
  private class Visitor implements RSQLVisitor<Boolean, EventPayload> {

    /**
     * Visit an AND node. Result is the AND of all the children trees evaluation
     *
     * @param node The current AND node
     * @param param The payload read from the Kafka event
     * @return True if all the sub-trees evaluate true
     */
    @Override
    public Boolean visit(AndNode node, EventPayload param) {
      List<Node> children = node.getChildren();
      for (Node child : children) {
        if (child instanceof AndNode) {
          if (Boolean.FALSE.equals(visit((AndNode) child, param))) {
            return false;
          }
        } else if (child instanceof OrNode) {
          if (Boolean.FALSE.equals(visit((OrNode) child, param))) {
            return false;
          }
        } else {
          if (Boolean.FALSE.equals(visit((ComparisonNode) child, param))) {
            return false;
          }
        }
      }
      return true;
    }

    /**
     * Visit an OR node. Result is the OR of all the children trees evaluation
     *
     * @param node The current OR node
     * @param param The payload read from the Kafka event
     * @return True if at least one of the sub-trees evaluate true
     */
    @Override
    public Boolean visit(OrNode node, EventPayload param) {
      List<Node> children = node.getChildren();
      for (Node child : children) {
        if (child instanceof AndNode) {
          if (Boolean.TRUE.equals(visit((AndNode) child, param))) {
            return true;
          }
        } else if (child instanceof OrNode) {
          if (Boolean.TRUE.equals(visit((OrNode) child, param))) {
            return true;
          }
        } else {
          if (Boolean.TRUE.equals(visit((ComparisonNode) child, param))) {
            return true;
          }
        }
      }
      return false;
    }

    /**
     * Visit a comparison node, implementing the needed comparison operations according to the operator in the node and the
     * associated operands.
     *
     * @param node The comparison node of the evaluation tree
     * @param param The payload read from the kafka event
     * @return True if the comparison was successful
     */
    @Override
    public Boolean visit(ComparisonNode node, EventPayload param) {
      Object selectorValue = param.getJsonAttrValue(node.getSelector());
      ComparisonOperator op = node.getOperator();
      switch (op.getSymbol()) {
        case "==":
          return checkEquals(selectorValue, node.getArguments());
        case "!=":
          return checkNotEquals(selectorValue, node.getArguments());
        case ">":
        case "=gt=":
          return checkGreater(selectorValue, node.getArguments());
        case ">=":
        case "=ge=":
          return checkGreaterEquals(selectorValue, node.getArguments());
        case "<":
        case "=lt=":
          return checkLesser(selectorValue, node.getArguments());
        case "<=":
        case "=le=":
          return checkLesserEquals(selectorValue, node.getArguments());
        case "=in=":
          return checkPartOf(selectorValue, node.getArguments());
        case "=out=":
          return checkNotPartOf(selectorValue, node.getArguments());
        case "=regex=":
          return checkRegexp(selectorValue, node.getArguments());  
        default:
          return false;
      }
    }
    
    private boolean checkEquals(Object value, List<String> filterValues) {
      if (value instanceof String) {
        return ((String) value).equals(filterValues.get(0));
      } else if (value instanceof Integer) {
        return ((Integer) value).equals(getInteger(filterValues.get(0)));
      } else if (value instanceof Double) {
        return ((Double) value).equals(getDouble(filterValues.get(0)));
      } else if (value instanceof Boolean) {
        return (Boolean) value == Boolean.parseBoolean(filterValues.get(0));
      } else if (value instanceof JSONArray) {
        for (Object item: ((JSONArray) value).subList(0, ((JSONArray) value).size())) {
          if (checkEquals(item, filterValues)) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean checkNotEquals(Object value, List<String> filterValues) {
      return !checkEquals(value, filterValues);
    }

    private boolean checkGreater(Object value, List<String> filterValues) {
      if (value instanceof String) {
        return ((String) value).compareTo(filterValues.get(0)) > 0;
      } else if (value instanceof Integer) {
        return (Integer) value > getInteger(filterValues.get(0));
      } else if (value instanceof Double) {
        return (Double) value > getDouble(filterValues.get(0));
      } else if (value instanceof JSONArray) {
        for (Object item: ((JSONArray) value).subList(0, ((JSONArray) value).size())) {
          if (checkGreater(item, filterValues)) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean checkGreaterEquals(Object value, List<String> filterValues) {
      if (value instanceof String) {
        return ((String) value).compareTo(filterValues.get(0)) >= 0;
      } else if (value instanceof Integer) {
        return (Integer) value >= getInteger(filterValues.get(0));
      } else if (value instanceof Double) {
        return (Double) value >= getDouble(filterValues.get(0));
      } else if (value instanceof JSONArray) {
        for (Object item: ((JSONArray) value).subList(0, ((JSONArray) value).size())) {
          if (checkGreaterEquals(item, filterValues)) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean checkLesser(Object value, List<String> filterValues) {
      if (value instanceof String) {
        return ((String) value).compareTo(filterValues.get(0)) < 0;
      } else if (value instanceof Integer) {
        return (Integer) value < getInteger(filterValues.get(0));
      } else if (value instanceof Double) {
        return (Double) value < getDouble(filterValues.get(0));
      } else if (value instanceof JSONArray) {
        for (Object item: ((JSONArray) value).subList(0, ((JSONArray) value).size())) {
          if (checkLesser(item, filterValues)) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean checkLesserEquals(Object value, List<String> filterValues) {
      if (value instanceof String) {
        return ((String) value).compareTo(filterValues.get(0)) <= 0;
      } else if (value instanceof Integer) {
        return (Integer) value <= getInteger(filterValues.get(0));
      } else if (value instanceof Double) {
        return (Double) value <= getDouble(filterValues.get(0));
      } else if (value instanceof JSONArray) {
        for (Object item: ((JSONArray) value).subList(0, ((JSONArray) value).size())) {
          if (checkLesserEquals(item, filterValues)) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean checkPartOf(Object value, List<String> filterValues) {
      if (value instanceof String) {
        Set<String> values = new HashSet<>(filterValues);
        return values.contains(value);
      } else if (value instanceof Integer) {
        Set<Integer> values = new HashSet<>();
        filterValues.forEach(arg -> values.add(getInteger(arg)));
        return values.contains(value);
      } else if (value instanceof Double) {
        Set<Double> values = new HashSet<>();
        filterValues.forEach(arg -> values.add(getDouble(arg)));
        return values.contains(value);
      } else if (value instanceof JSONArray) {
        for (Object item: ((JSONArray) value).subList(0, ((JSONArray) value).size())) {
          if (checkPartOf(item, filterValues)) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean checkNotPartOf(Object value, List<String> filterValues) {
      return !checkPartOf(value, filterValues);
    }

    private boolean checkRegexp(Object value, List<String>filterValues) {
      var pattern = Pattern.compile(filterValues.get(0));
      if (value instanceof String) {
        var matcher = pattern.matcher((String) value);
        return matcher.matches();      
      } else if (value instanceof JSONArray) {
        for (Object item: ((JSONArray) value).subList(0, ((JSONArray) value).size())) {
          if (checkRegexp(item, filterValues)) {
            return true;
          }
        }
      }
      return false;
    }

    /**
     * Parse a string supposed to contain an integer
     *
     * @param value The long value string
     * @return The long as an optional
     */
    private Integer getInteger(String value) {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException | NullPointerException e) {
        return null;
      }
    }

    /**
     * Parse a string supposed to contain a double
     *
     * @param value The long value string
     * @return The long as an optional
     */
    private Double getDouble(String value) {
      try {
        return Double.parseDouble(value);
      } catch (NumberFormatException | NullPointerException e ) {
        return null;
      }
    }
  }

  /**
   * The FilterParser constructor.
   *
   * @param queryString The string with the filter.
   */
  public RsqlFilter(String queryString) {
    Set<ComparisonOperator> operators = RSQLOperators.defaultOperators();
    operators.add(new ComparisonOperator("=regex=", false));

    try {
      root = new RSQLParser(operators).parse(queryString);
    } catch (RSQLParserException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  /**
   * Check if the given payload matches the filter.
   *
   * @param payload The payload read from the kafka event
   * @return True if the payload matches the event, false otherwise
   */
  public Boolean eval(EventPayload payload) {
    return root.accept(new Visitor(), payload);
  }

  /**
   * Check if the provided string is a valid filter. This method is static, so there is no need to try and allocate a new
   * FilterParser to use it.
   *
   * @param queryString The filter string
   * @return True if the string is a valid filter, false otherwise
   */
  public static boolean isValid(String queryString) {
    try {
      new RSQLParser().parse(queryString);
    } catch (RSQLParserException e) {
      return false;
    }
    return true;
  }
}
