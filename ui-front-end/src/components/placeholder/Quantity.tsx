import React, { useEffect, useState } from "react";
import "./QuantityCell.css";

interface QuantityCellProps {
  quantity: number;
  previousQuantity: number;
  side: "bid" | "ask";
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
      setBarWidth(Math.abs(change));
    }
  }, [quantity, previousQuantity]);

  return (
    <td>
      <div className={`quantity-bar ${side}`}>
        <div className="bar" style={{ width: `${barWidth}%` }}></div>
        {quantity}
      </div>
    </td>
  );
};
