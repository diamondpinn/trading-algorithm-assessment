import React from "react";
import "./Arrow.css";

type ArrowProps = {
  direction: "up" | "down";
  color: string;
};

const Arrow: React.FC<ArrowProps> = ({ direction, color }) => {
  return (
    <span
      className={`arrow-indicator ${
        direction === "up" ? "arrow-up" : "arrow-down"
      }`}
      style={{ color }}
    >
      {direction === "up" ? "▲" : "▼"}{" "}
      {/* Arrow character based on direction */}
    </span>
  );
};

export default Arrow;
