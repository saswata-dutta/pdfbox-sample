package org.saswata;

public class AwsBillLineItem {
  public final String item;
  public final int amount;

  public AwsBillLineItem(String item, int number) {
    this.item = item;
    this.amount = number;
  }

  @Override
  public String toString() {
    return String.format("{%s = %s}", item, amount);
  }
}
