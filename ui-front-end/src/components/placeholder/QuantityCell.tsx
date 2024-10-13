import React, { useEffect, useState } from "react";
import "./QuantityCell.css"; // Import your CSS

interface QuantityCellProps {
  quantity: number;
  previousQuantity: number; // To calculate the change
  side: "bid" | "ask"; // To determine the side of the quantity
}

export const QuantityCell: React.FC<QuantityCellProps> = ({
  quantity,
  previousQuantity,
  side,
}) => {
  const [barWidth, setBarWidth] = useState(0);

  useEffect(() => {
    if (previousQuantity > 0) {
      const change = ((quantity - previousQuantity) / previousQuantity) * 100;
      setBarWidth(Math.abs(change)); // Bar width based on percentage change
    }
  }, [quantity, previousQuantity]);

  return (
    <td>
      <div className={quantity-bar ${side}}>
        <div className="bar" style={{ width: ${barWidth}% }}></div>
        {quantity}
      </div>
    </td>
  );
};