import React from "react";
import "./Arrow.css"; // Import the CSS for arrow styles
type ArrowProps = {
  direction: "up" | "down";
  color: string;
};
const Arrow: React.FC<ArrowProps> = ({ direction, color }) => {
  return (
    <span
      className={arrow-indicator ${
        direction === "up" ? "arrow-up" : "arrow-down"
      }}
      style={{ color }}
    >
      {direction === "up" ? "▲" : "▼"}
    </span>
  );
};
export default Arrow;