import { useState } from "react";
import "./MarketDataPanel.css";
import MarketTable from "./MarketTable";
import { MarketDepthRow } from "../market-depth/useMarketDepthData";

export interface Props {
  data: MarketDepthRow[];
}

export const MarketDataPanel = ({ data }: Props) => {
  const [displayState, setDisplayState] = useState<"instructions" | "design">(
    "instructions"
  );

  const toggleDisplayState = () => {
    setDisplayState((prev) =>
      prev === "instructions" ? "design" : "instructions"
    );
  };
  const buttonLabel =
    displayState === "instructions" ? "design" : "instructions";
  return (
    <div className="MarketDataPanel">
      {" "}
      {displayState === "instructions" ? (
        <div className="MarketDataPanel-instructions">
          <span>Market data will be displayed here</span>{" "}
        </div>
      ) : (
        <div className="MarketDataPanel-design">
          {/* Display the MarketTable component */}
          <MarketTable data={data} />{" "}
        </div>
      )}{" "}
      <div className="MarketDataPanel-buttonContainer">
        {" "}
        <button onClick={toggleDisplayState}>
          Click to view {buttonLabel}{" "}
        </button>{" "}
      </div>{" "}
    </div>
  );
};
export default MarketDataPanel;
