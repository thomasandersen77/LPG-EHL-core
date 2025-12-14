import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  DispenserDto,
  fetchDispenserState,
  stopDispenser,
  unblockDispenser,
} from './api';

function formatN(value: number) {
  return value.toLocaleString('nb-NO', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function App() {
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery<DispenserDto>({
    queryKey: ['dispenser'],
    queryFn: fetchDispenserState,
    refetchInterval: 1000, // poll hvert sekund i dev
  });

  const unblockMutation = useMutation({
    mutationFn: unblockDispenser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['dispenser'] }),
  });

  const stopMutation = useMutation({
    mutationFn: stopDispenser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['dispenser'] }),
  });

  const disabled =
    isLoading || unblockMutation.isPending || stopMutation.isPending;

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="w-full max-w-5xl bg-white shadow-xl rounded-2xl p-8 space-y-6">
        <header className="flex items-center justify-between border-b pb-4">
          <div>
            <h1 className="text-2xl font-semibold">LPG Pumpestyring</h1>
            <p className="text-sm text-slate-500">
              Lokal test mot emulator / API
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-sm text-slate-600">Avgift:</span>
            <input
              type="number"
              defaultValue={0}
              step={0.01}
              className="w-24 rounded-md border border-slate-300 px-2 py-1 text-right text-sm"
              readOnly
              // TODO: koble på API når avgift blir en first-class verdi
            />
          </div>
        </header>

        {/* Main content */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Left: metrics */}
          <div className="md:col-span-2 grid grid-rows-3 gap-4">
            <MetricCard label="Beløp å betale" value={data?.amountToPay} />
            <MetricCard label="Antall liter" value={data?.litres} />
            <MetricCard label="Pris kr/l" value={data?.pricePerLitre} />
            <div className="mt-1">
              <label className="inline-flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={!!data?.includeRoadTax}
                  readOnly
                  className="h-4 w-4"
                />
                <span className="px-2 py-1 rounded bg-roadTaxYellow text-xs font-medium">
                  Inkl vegbruksavgift
                </span>
              </label>
            </div>
          </div>

          {/* Right: controls */}
          <div className="flex flex-col justify-center gap-4">
            <button
              className="w-full rounded-xl py-4 text-lg font-semibold text-white bg-primaryGreen hover:brightness-110 disabled:opacity-60 disabled:cursor-not-allowed"
              disabled={disabled || data?.state === 'DELIVERING'}
              onClick={() => unblockMutation.mutate()}
            >
              Frigi dispenser
            </button>
            <button
              className="w-full rounded-xl py-4 text-lg font-semibold text-white bg-primaryRed hover:brightness-110 disabled:opacity-60 disabled:cursor-not-allowed"
              disabled={disabled || data?.state !== 'DELIVERING'}
              onClick={() => stopMutation.mutate()}
            >
              Stopp dispenser
            </button>

            <div className="mt-4 space-y-2 text-sm">
              <ModeCheckbox label="Kort aktiv" checked={!!data?.cardModeActive} />
              <ModeCheckbox label="Dag modus" checked={!!data?.dayMode} />
              <ModeCheckbox
                label="Stasjonskreditt aktiv"
                checked={!!data?.stationCreditActive}
              />
            </div>
          </div>
        </div>

        {/* Bottom status bar */}
        <footer className="border-t pt-3 flex items-center justify-between text-xs">
          <div>
            {isError ? (
              <span className="px-2 py-1 rounded bg-primaryRed text-white">
                Ingen kontakt med dispenser / API
              </span>
            ) : data?.connected ? (
              <span className="px-2 py-1 rounded bg-green-100 text-green-800">
                Tilkoblet til dispenser (emulator)
              </span>
            ) : (
              <span className="px-2 py-1 rounded bg-yellow-100 text-yellow-800">
                Venter på tilkobling…
              </span>
            )}
          </div>

          <div className="text-slate-400">
            State:{' '}
            <span className="font-mono">
              {isLoading ? 'LASTER…' : data?.state ?? 'UKJENT'}
            </span>
          </div>
        </footer>
      </div>
    </div>
  );
}

type MetricProps = {
  label: string;
  value?: number;
};

function MetricCard({ label, value }: MetricProps) {
  return (
    <div className="flex flex-col">
      <span className="text-sm text-slate-600 mb-1">{label}</span>
      <div className="flex-1 bg-paleBlue rounded-xl border border-slate-200 flex items-center justify-end px-6">
        <span className="text-4xl md:text-5xl font-semibold tracking-tight">
          {value !== undefined ? formatN(value) : '--'}
        </span>
      </div>
    </div>
  );
}

type ModeCheckboxProps = {
  label: string;
  checked: boolean;
};

function ModeCheckbox({ label, checked }: ModeCheckboxProps) {
  return (
    <label className="inline-flex items-center gap-2 text-slate-700">
      <input type="checkbox" checked={checked} readOnly className="h-4 w-4" />
      <span>{label}</span>
    </label>
  );
}

export default App;
