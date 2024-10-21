import React, { useEffect, useState } from "react";
import "./PriceCell.css";

interface PriceCellProps {
  price: number;
}

export const PriceCell: React.FC<PriceCellProps> = (props) => {
  const [previousPrice, setPreviousPrice] = useState<number>(props.price);
  const [direction, setDirection] = useState<"up" | "down" | "neutral">(
    "neutral"
  );

  useEffect(() => {
    if (props.price > previousPrice) {
      setDirection("up");
    } else if (props.price < previousPrice) {
      setDirection("down");
    } else {
      setDirection("neutral");
    }

    setPreviousPrice(props.price);
  }, [props.price, previousPrice]);

  return (
    <td>
      <span className={`arrow ${direction}`}>
        {/* Arrow character based on direction */}
        {direction === "up" ? "↑" : direction === "down" ? "↓" : "-"}
      </span>
      {props.price}
    </td>
  );
};
