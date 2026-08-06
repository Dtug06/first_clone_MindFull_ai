import { motion } from 'framer-motion';

interface SoftLineChartProps {
  data: { day: string; score: number }[];
  height?: number;
  color?: string;
  /**
   * When true, the chart doubles its height on lg+ screens while keeping
   * the original (mobile) size on smaller breakpoints.
   * The mobile height is whatever `height` is passed in (or the default 220).
   */
  responsive?: boolean;
  /**
   * Optional override for day labels rendered under each point. When provided,
   * `data[i].day` is replaced by `dayLabels[i]` in the SVG. Useful for i18n.
   */
  dayLabels?: string[];
}
export default function SoftLineChart({
  data,
  height = 220,
  color = '#5F9E97',
  responsive = false,
  dayLabels,
}: SoftLineChartProps) {
  const maxScore = Math.max(...data.map((d) => d.score));
  const minScore = Math.min(...data.map((d) => d.score));
  const range = maxScore - minScore || 1;
  const paddedMin = Math.max(0, minScore - range * 0.3);
  const paddedMax = maxScore + range * 0.3;
  const paddedRange = paddedMax - paddedMin || 1;

  // Increased padding so labels have room and chart is clearly visible
  const padding = { left: 12, right: 12, top: 16, bottom: 28 };
  const chartWidth = 100 - padding.left - padding.right;
  const chartHeight = 100 - padding.top - padding.bottom;

  const points = data.map((d, i) => ({
    x: padding.left + (i / Math.max(1, data.length - 1)) * chartWidth,
    y: padding.top + chartHeight - ((d.score - paddedMin) / paddedRange) * chartHeight,
  }));

  const pathD = points.reduce((acc, point, i) => {
    if (i === 0) return `M ${point.x} ${point.y}`;
    const prev = points[i - 1];
    const cp1x = prev.x + (point.x - prev.x) / 3;
    const cp2x = point.x - (point.x - prev.x) / 3;
    return `${acc} C ${cp1x} ${prev.y}, ${cp2x} ${point.y}, ${point.x} ${point.y}`;
  }, '');

  const areaD = `${pathD} L ${padding.left + chartWidth} ${padding.top + chartHeight} L ${padding.left} ${padding.top + chartHeight} Z`;

  // For responsive mode, we use a CSS custom property so the height can scale
  // on lg+ screens without requiring Tailwind to know the dynamic value.
  // The @media rule lives in index.css and reads --chart-height.
  const style = responsive
    ? ({
        ['--chart-height-mobile' as string]: `${height}px`,
        ['--chart-height-desktop' as string]: `${height * 2}px`,
        height: 'var(--chart-height-mobile)',
      } as React.CSSProperties)
    : ({ height } as React.CSSProperties);

  return (
    <div className="w-full" style={style}>
      <svg
        viewBox="0 0 100 100"
        className="w-full h-full"
        preserveAspectRatio="xMidYMid meet"
      >
        <defs>
          <linearGradient id={`lineGradient-${color.replace('#', '')}`} x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor={color} stopOpacity="0.35" />
            <stop offset="100%" stopColor={color} stopOpacity="0.02" />
          </linearGradient>
        </defs>

        {/* Area fill */}
        <motion.path
          d={areaD}
          fill={`url(#lineGradient-${color.replace('#', '')})`}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 1, delay: 0.3 }}
        />

        {/* Line */}
        <motion.path
          d={pathD}
          fill="none"
          stroke={color}
          strokeWidth="0.8"
          strokeLinecap="round"
          strokeLinejoin="round"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 1.5, ease: 'easeOut' }}
        />

        {/* Points */}
        {points.map((point, i) => (
          <g key={i}>
            <motion.circle
              cx={point.x}
              cy={point.y}
              r="1.8"
              fill="white"
              stroke={color}
              strokeWidth="0.6"
              initial={{ opacity: 0, scale: 0 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.3, delay: 0.5 + i * 0.1 }}
            />
          </g>
        ))}

        {/* Day labels */}
        {data.map((d, i) => (
          <text
            key={i}
            x={points[i].x}
            y="98"
            textAnchor="middle"
            style={{
              fill: '#6E7772',
              fontSize: '4px',
              fontFamily: 'Inter, system-ui, sans-serif',
              fontWeight: 500,
            }}
          >
            {dayLabels?.[i] ?? d.day}
          </text>
        ))}
      </svg>
    </div>
  );
}
