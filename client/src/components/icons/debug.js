import React from "react";

export class DebugSVG extends React.Component {
    render() {
        return (
            <svg className={"inline text-gray-400 h-8 w-8"} width="24" height="24" viewBox="0 0 24 24" strokeWidth="2"
                 stroke="currentColor" fill="none" strokeLinecap="round" strokeLinejoin="round">
                <path stroke="none" d="M0 0h24v24H0z"/>
                <path d="M9 9v-1a3 3 0 0 1 6 0v1"/>
                <path d="M8 9h8a6 6 0 0 1 1 3v3a5 5 0 0 1 -10 0v-3a6 6 0 0 1 1 -3"/>
                <line x1="3" y1="13" x2="7" y2="13"/>
                <line x1="17" y1="13" x2="21" y2="13"/>
                <line x1="12" y1="20" x2="12" y2="14"/>
                <line x1="4" y1="19" x2="7.35" y2="17"/>
                <line x1="20" y1="19" x2="16.65" y2="17"/>
                <line x1="4" y1="7" x2="7.75" y2="9.4"/>
                <line x1="20" y1="7" x2="16.25" y2="9.4"/>
            </svg>
        );
    }
}